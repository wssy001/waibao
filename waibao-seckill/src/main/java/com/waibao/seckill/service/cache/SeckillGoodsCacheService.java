package com.waibao.seckill.service.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.mapper.SeckillGoodsMapper;
import com.waibao.util.async.AsyncService;
import com.waibao.util.base.RedisCommand;
import com.waibao.util.tools.BigDecimalValueFilter;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * GoodsRetailerCacheService
 *
 * @author alexpetertyler
 * @since 2022/3/4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillGoodsCacheService {
    public static final String REDIS_SECKILL_GOODS_KEY_PREFIX = "seckill-goods-";
    public static final String REDIS_SECKILL_GOODS_STATUS_KEY = "seckill-goods-status";

    private final AsyncService asyncService;
    private final SeckillGoodsMapper seckillGoodsMapper;

    @Resource
    private RedisTemplate<String, SeckillGoods> goodsRedisTemplate;

    private RedisScript<String> canalSync;
    private BloomFilter<Long> bloomFilter;
    private RedisScript<Boolean> getGoodsStatus;
    private RedisScript<String> getSeckillGoods;
    private RedisScript<String> batchInsertGoods;
    private RedisScript<String> updateGoodsStatus;
    private Cache<Long, Boolean> goodsStatusCache;
    private RedisScript<String> insertSeckillGoods;
    private RedisScript<String> batchRollBackStorage;
    private RedisScript<Boolean> decreaseGoodsStorage;
    private RedisScript<String> batchUpdateGoodsStatus;
    private Cache<Long, SeckillGoods> seckillGoodsCache;

    @PostConstruct
    void init() {
        bloomFilter = BloomFilter.create(Funnels.longFunnel(), 10L, 0.1);
        getGoodsStatus = RedisScript.of(new ClassPathResource("lua/getGoodsStatus.lua"), Boolean.class);
        getSeckillGoods = RedisScript.of(new ClassPathResource("lua/getGoodsScript.lua"), String.class);
        canalSync = RedisScript.of(new ClassPathResource("lua/canalSyncGoodsScript.lua"), String.class);
        updateGoodsStatus = RedisScript.of(new ClassPathResource("lua/updateGoodsStatus.lua"), String.class);
        insertSeckillGoods = RedisScript.of(new ClassPathResource("lua/insertGoodsScript.lua"), String.class);
        batchInsertGoods = RedisScript.of(new ClassPathResource("lua/batchInsertGoodsScript.lua"), String.class);
        decreaseGoodsStorage = RedisScript.of(new ClassPathResource("lua/decreaseGoodsStorage.lua"), Boolean.class);
        batchUpdateGoodsStatus = RedisScript.of(new ClassPathResource("lua/batchUpdateGoodsStatus.lua"), String.class);
        batchRollBackStorage = RedisScript.of(new ClassPathResource("lua/batchRollBackGoodsStorage.lua"), String.class);

        seckillGoodsCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(300)
                .build();
        goodsStatusCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }

    public void updateGoodsStatus(Long goodsId, boolean finished) {
        goodsStatusCache.put(goodsId, finished);
        goodsRedisTemplate.execute(updateGoodsStatus, Collections.singletonList(REDIS_SECKILL_GOODS_STATUS_KEY), goodsId + "", finished + "");
    }

    public void updateBatchGoodsStatus(List<SeckillGoods> seckillGoodsList) {
        Date now = new Date();
        Map<Long, Boolean> collect = seckillGoodsList.stream()
                .collect(Collectors.toMap(SeckillGoods::getId, seckillGoods -> !now.before(seckillGoods.getSeckillEndTime())));
        goodsStatusCache.asMap()
                .putAll(collect);
        goodsRedisTemplate.execute(batchUpdateGoodsStatus, Collections.singletonList(REDIS_SECKILL_GOODS_STATUS_KEY), JSONArray.toJSONString(seckillGoodsList));
    }

    public boolean finished(Long goodsId) {
        Boolean present = goodsStatusCache.get(goodsId, key -> Boolean.FALSE.equals(goodsRedisTemplate.execute(getGoodsStatus, Collections.singletonList(REDIS_SECKILL_GOODS_STATUS_KEY), goodsId + "")));

        if (present == null) {
            SeckillGoods seckillGoods = get(goodsId);
            if (seckillGoods == null) {
                goodsStatusCache.put(goodsId, true);
                return true;
            } else {
                present = seckillGoods.getStorage() <= 0;
                goodsStatusCache.put(goodsId, present);
                return present;
            }
        }

        return Boolean.TRUE.equals(present);
    }

    public SeckillGoods get(Long goodsId) {
        SeckillGoods seckillGoods = seckillGoodsCache.getIfPresent(goodsId);
        if (seckillGoods != null) return seckillGoods;

        String execute = goodsRedisTemplate.execute(getSeckillGoods, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX), goodsId + "");
        if (!"{}".equals(execute)) seckillGoods = JSON.parseObject(execute, SeckillGoods.class);
        if (seckillGoods != null) {
            set(seckillGoods, false);
            return seckillGoods;
        }
        if (!bloomFilter.mightContain(goodsId)) return null;

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
        bloomFilter.put(goodsId);
        if (updateRedis)
            goodsRedisTemplate.execute(insertSeckillGoods, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX), JSON.toJSONString(seckillGoods));
    }

    public void insertBatch(List<SeckillGoods> seckillGoodsList) {
        asyncService.basicTask(() -> updateBatchGoodsStatus(seckillGoodsList));
        asyncService.basicTask(() -> seckillGoodsList.forEach(seckillGoods -> bloomFilter.put(seckillGoods.getGoodsId())));
        asyncService.basicTask(() -> goodsRedisTemplate.execute(batchInsertGoods, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX), JSONArray.toJSONString(seckillGoodsList, new BigDecimalValueFilter())));
    }

    public List<OrderVO> batchRollBackStorage(List<OrderVO> orderVOList) {
        String arrayString = goodsRedisTemplate.execute(batchRollBackStorage, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX), JSONArray.toJSONString(orderVOList, new BigDecimalValueFilter()));
        return "{}".equals(arrayString) ? new ArrayList<>() : JSONArray.parseArray(arrayString, OrderVO.class);
    }

    public boolean decreaseStorage(Long goodsId, int count) {
        return Boolean.TRUE.equals(goodsRedisTemplate.execute(decreaseGoodsStorage, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX), goodsId + "", count + ""));
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        asyncService.basicTask(() -> goodsRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX), JSONArray.toJSONString(redisCommandList, new BigDecimalValueFilter())));
    }
}
