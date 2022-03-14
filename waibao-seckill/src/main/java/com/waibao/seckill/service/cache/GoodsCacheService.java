package com.waibao.seckill.service.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.mapper.SeckillGoodsMapper;
import com.waibao.util.base.RedisCommand;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * GoodsRetailerCacheService
 *
 * @author alexpetertyler
 * @since 2022/3/4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsCacheService {
    public static final String REDIS_SECKILL_GOODS_KEY_PREFIX = "seckill-goods-";

    private final RedissonClient redissonClient;
    private final SeckillGoodsMapper seckillGoodsMapper;

    @Resource
    private RedisTemplate<String, SeckillGoods> goodsRedisTemplate;

    private RBloomFilter<Long> bloomFilter;
    private DefaultRedisScript<String> canalSync;
    private DefaultRedisScript<String> setSeckillGoods;
    private Cache<Long, SeckillGoods> seckillGoodsCache;
    private Cache<Long, Boolean> seckillFinishCache;
    private DefaultRedisScript<Boolean> decreaseStorage;
    private DefaultRedisScript<String> batchRollBackStorage;
    private DefaultRedisScript<SeckillGoods> getSeckillGoods;
    private DefaultRedisScript<String> batchInsertOrderGoods;

    @PostConstruct
    void init() {
        String insertGoodsScript = "local key = KEYS[1]\n" +
                "local goodsId = seckillGoods['goodsId']\n" +
                "local seckillGoods = cjson.decode(ARGV[1])\n" +
                "redis.call('HSET', key .. goodsId, 'id', seckillGoods['id'], 'goodsId', goodsId, 'retailerId',\n" +
                "        seckillGoods['retailerId'], 'price', seckillGoods['price'], 'seckillPrice',\n" +
                "        seckillGoods['seckillPrice'], 'storage', seckillGoods['storage'], 'purchaseLimit',\n" +
                "        seckillGoods['purchaseLimit'], 'seckillEndTime', seckillGoods['seckillEndTime'],\n" +
                "        '@type', 'com.waibao.seckill.entity.SeckillGoods')";
        String batchInsertGoodsScript = "local key = KEYS[1]\n" +
                "local seckillGoodsList = {}\n" +
                "local seckillGoods\n" +
                "local goodsId\n" +
                "for _, value in ipairs(ARGV) do\n" +
                "    seckillGoods = cjson.decode(value)\n" +
                "    goodsId = seckillGoods['goodsId']\n" +
                "    if next(redis.call('HKEYS', key .. goodsId)) then\n" +
                "        table.insert(seckillGoodsList, seckillGoods)\n" +
                "    else\n" +
                "        redis.call('HSET', key .. goodsId, 'id', seckillGoods['id'], 'goodsId', goodsId, 'retailerId',\n" +
                "                seckillGoods['retailerId'], 'price', seckillGoods['price'], 'seckillPrice',\n" +
                "                seckillGoods['seckillPrice'], 'storage', seckillGoods['storage'], 'purchaseLimit',\n" +
                "                seckillGoods['purchaseLimit'], 'seckillEndTime', seckillGoods['seckillEndTime'],\n" +
                "                '@type', 'com.waibao.seckill.entity.SeckillGoods')\n" +
                "    end\n" +
                "end\n" +
                "\n" +
                "if not next(seckillGoodsList) then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(seckillGoodsList)\n" +
                "end";
        String getGoodsScript = "local key = KEYS[1]\n" +
                "local seckillGoods = {}\n" +
                "local goodsId = ARGV[1]\n" +
                "local seckillGoodsKeys = redis.call('HVALS', key .. goodsId)\n" +
                "for _, value in ipairs(seckillGoodsKeys) do\n" +
                "    seckillGoods[value] = redis.call('HGET', key .. goodsId, value)\n" +
                "end\n" +
                "\n" +
                "return cjson.encode(seckillGoods)";
        String canalSyncGoodsScript = "local key = KEYS[1]\n" +
                "local redisCommand\n" +
                "local seckillGoods\n" +
                "local goodsId\n" +
                "for _, value in ipairs(ARGV) do\n" +
                "    redisCommand = cjson.decode(value)\n" +
                "    seckillGoods = redisCommand['value']\n" +
                "    goodsId = seckillGoods['goodsId']\n" +
                "    key = '\"' .. string.gsub(key, '\"', '') .. seckillGoods['goodsId'] .. '\"'\n" +
                "    if redisCommand['command'] == 'SET' then\n" +
                "        redis.call('HSET', key, 'id', seckillGoods['id'], 'goodsId', goodsId, 'retailerId',\n" +
                "                seckillGoods['retailerId'], 'price', seckillGoods['price'], 'seckillPrice',\n" +
                "                seckillGoods['seckillPrice'], 'storage', seckillGoods['storage'], 'purchaseLimit',\n" +
                "                seckillGoods['purchaseLimit'], 'seckillEndTime', seckillGoods['seckillEndTime'],\n" +
                "                '@type', 'com.waibao.seckill.entity.SeckillGoods')\n" +
                "    else\n" +
                "        redis.call('DEL', key)\n" +
                "    end\n" +
                "end";
        String decreaseStorageScript = "local key = KEYS[1]\n" +
                "local goodsId = ARGV[1]\n" +
                "local count = ARGV[2]\n" +
                "if tonumber(redis.call('HEGT', key .. goodsId, 'purchaseLimit')) < count then\n" +
                "    return false\n" +
                "end\n" +
                "if tonumber(redis.call('HINCRBY', key .. goodsId, 'storage', -count)) < 0 then\n" +
                "    redis.call('HINCRBY', key .. goodsId, 'storage', count)\n" +
                "    return false\n" +
                "else\n" +
                "    return true\n" +
                "end";
        String batchRollBackStorageScript = "local key = KEYS[1]\n" +
                "local orderVO\n" +
                "local orderVOList = {}\n" +
                "local goodsId\n" +
                "for _, value in ipairs(ARGV) do\n" +
                "    orderVO = cjson.decode(value)\n" +
                "    goodsId = orderVO['goodsId']\n" +
                "    if next(redis.call('HKEYS', key .. goodsId)) then\n" +
                "        orderVO['status'] = '库存回滚失败'\n" +
                "        table.insert(orderVOList, orderVO)\n" +
                "    else\n" +
                "        redis.call('HINCRBY', key .. goodsId, 'storage', orderVO['count'])\n" +
                "        orderVO['status'] = '库存回滚成功'\n" +
                "    end\n" +
                "end\n" +
                "\n" +
                "if not next(orderVOList) then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(orderVOList)\n" +
                "end";
        bloomFilter = redissonClient.getBloomFilter("orderGoodsList");
        bloomFilter.tryInit(10000L, 0.01);
        batchInsertOrderGoods = new DefaultRedisScript<>(batchInsertGoodsScript, String.class);
        setSeckillGoods = new DefaultRedisScript<>(insertGoodsScript);
        getSeckillGoods = new DefaultRedisScript<>(getGoodsScript, SeckillGoods.class);
        decreaseStorage = new DefaultRedisScript<>(decreaseStorageScript, Boolean.class);
        batchRollBackStorage = new DefaultRedisScript<>(batchRollBackStorageScript, String.class);
        canalSync = new DefaultRedisScript<>(canalSyncGoodsScript);
        seckillGoodsCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(300)
                .build();
        seckillFinishCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(300)
                .build();
    }

    public void updateGoodsStatus(Long goodsId, boolean finished) {
        seckillFinishCache.put(goodsId, finished);
    }

    public boolean finished(Long goodsId) {
        Boolean present = seckillFinishCache.getIfPresent(goodsId);
        if (present == null) {
            SeckillGoods seckillGoods = get(goodsId);
            if (seckillGoods == null) {
                seckillFinishCache.put(goodsId, true);
                return true;
            } else {
                present = seckillGoods.getStorage() <= 0;
                seckillFinishCache.put(goodsId, present);
                return present;
            }
        }

        return Boolean.TRUE.equals(present);
    }

    public SeckillGoods get(Long goodsId) {
        SeckillGoods seckillGoods = seckillGoodsCache.getIfPresent(goodsId);
        if (seckillGoods != null) return seckillGoods;

        if (!bloomFilter.contains(goodsId)) return null;

        seckillGoods = goodsRedisTemplate.execute(getSeckillGoods, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX), goodsId);
        if (seckillGoods != null) {
            set(seckillGoods, false);
            return seckillGoods;
        }

        seckillGoods = seckillGoodsMapper.selectOne(Wrappers.<SeckillGoods>lambdaQuery().eq(SeckillGoods::getGoodsId, goodsId));
        if (seckillGoods != null) set(seckillGoods);

        return seckillGoods;
    }

    public void set(SeckillGoods seckillGoods) {
        set(seckillGoods, true);
    }

    public void set(SeckillGoods seckillGoods, boolean updateRedis) {
        Long goodsId = seckillGoods.getGoodsId();
        seckillGoodsCache.put(goodsId, seckillGoods);
        bloomFilter.add(goodsId);
        if (updateRedis)
            goodsRedisTemplate.execute(setSeckillGoods, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX), seckillGoods);
    }

    public List<SeckillGoods> insertBatch(List<SeckillGoods> seckillGoods) {
        String arrayString = goodsRedisTemplate.execute(batchInsertOrderGoods, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX),
                seckillGoods.toArray());
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, SeckillGoods.class);
    }

    public List<OrderVO> batchRollBackStorage(List<OrderVO> orderVOList) {
        String arrayString = goodsRedisTemplate.execute(batchRollBackStorage, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX), orderVOList.toArray());
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, OrderVO.class);
    }

    public boolean decreaseStorage(Long goodsId, int count) {
        return Boolean.TRUE.equals(goodsRedisTemplate.execute(decreaseStorage, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX), goodsId, count));
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        goodsRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX),
                redisCommandList.toArray());
    }
}
