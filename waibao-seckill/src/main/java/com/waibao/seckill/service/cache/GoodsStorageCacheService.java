package com.waibao.seckill.service.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.util.base.RedisCommand;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * SeckillGoodsCacheService
 *
 * @author alexpetertyler
 * @since 2022-02-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsStorageCacheService {
    public static final String REDIS_SECKILL_GOODS_KEY_PREFIX = "seckill-goods-storage-";

    @Resource
    private RedisTemplate<String, Integer> storageRedisTemplate;

    private ValueOperations<String, Integer> valueOperations;
    private DefaultRedisScript<String> batchInsertOrderGoods;
    private DefaultRedisScript<String> increaseStorage;
    private DefaultRedisScript<Boolean> decreaseStorage;
    private DefaultRedisScript<String> canalSync;

    @PostConstruct
    public void init() {
        String batchInsertScript = "local key = KEYS[1]\n" +
                "local seckillGoodsList = {}\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local seckillGoods = cjson.decode(value)\n" +
                "    local count = tonumber(redis.call('SETNX', key .. seckillGoods['goodsId'], seckillGoods['storage']))\n" +
                "    if count == 0 then\n" +
                "        table.insert(seckillGoodsList, seckillGoods)\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(seckillGoodsList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(seckillGoodsList)\n" +
                "end";
        String increaseScript = "local key = KEYS[1]\n" +
                "local orderVOList = {}\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local orderVO = cjson.decode(value)\n" +
                "    local count = orderVO['count']\n" +
                "    key = '\"' .. string.gsub(key, '\"', '') .. orderVO['goodsId'] .. '\"'\n" +
                "    if redis.call('GET', key) then\n" +
                "        table.insert(orderVOList, value)\n" +
                "    else\n" +
                "        redis.call('INCRBY', key, count)\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(orderVOList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(orderVOList)\n" +
                "end";
        String decreaseScript = "local key=KEYS[1]\n" +
                "local count = tonumber(ARGV[1])  \n" +
                "redis.call('DECRBY', KEYS[1], count)\n" +
                "local storage = tonumber(redis.call('GET',key))\n" +
                "if (storage < 0) then\n" +
                "redis.call('INCRBY',key,count)\n" +
                "return false\n" +
                "else return true\n" +
                "end";
        String canalSyncScript = "local key = KEYS[1]\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local redisCommand = cjson.decode(value)\n" +
                "    local seckillGoods = redisCommand['value']\n" +
                "    key = '\"' .. string.gsub(key, '\"', '') .. seckillGoods['goodsId'] .. '\"'\n" +
                "    if redisCommand['command'] == 'SET' then\n" +
                "        seckillGoods['@type'] = 'com.waibao.seckill.entity.SeckillGoods'\n" +
                "        redis.call('SET', key, seckillGoods['storage'])\n" +
                "    else\n" +
                "        redis.call('DEL', key)\n" +
                "    end\n" +
                "end";
        valueOperations = storageRedisTemplate.opsForValue();
        batchInsertOrderGoods = new DefaultRedisScript<>(batchInsertScript, String.class);
        increaseStorage = new DefaultRedisScript<>(increaseScript, String.class);
        decreaseStorage = new DefaultRedisScript<>(decreaseScript, Boolean.class);
        canalSync = new DefaultRedisScript<>(canalSyncScript);
    }

    public int get(Long goodsId) {
        Integer storage = valueOperations.get(REDIS_SECKILL_GOODS_KEY_PREFIX + goodsId);
        if (storage == null || storage < 1) return 0;
        return storage;
    }

    public List<SeckillGoods> insertBatch(List<SeckillGoods> seckillGoods) {
        String arrayString = storageRedisTemplate.execute(batchInsertOrderGoods, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX),
                seckillGoods.toArray());
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, SeckillGoods.class);
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        storageRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX),
                redisCommandList.toArray());
    }

    public List<OrderVO> increaseBatchStorage(List<OrderVO> orderVOList) {
        String arrayString = storageRedisTemplate.execute(increaseStorage, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX), orderVOList.toArray());
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, OrderVO.class);
    }

    public Boolean decreaseStorage(Long goodsId, int count) {
        return storageRedisTemplate.execute(decreaseStorage, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX + goodsId), count);
    }

    public void set(SeckillGoods seckillGoods) {
        valueOperations.set(REDIS_SECKILL_GOODS_KEY_PREFIX + seckillGoods.getGoodsId(), seckillGoods.getStorage());
    }

}
