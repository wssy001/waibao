package com.waibao.seckill.service.cache;

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
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * SeckillGoodsCacheService
 *
 * @author alexpetertyler
 * @since 2022-02-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoodsRetailerCacheService {
    public static final String REDIS_GOODS_RETAILER_KEY_PREFIX = "seckill-goods-retailer-";

    private final RedissonClient redissonClient;
    private final SeckillGoodsMapper seckillGoodsMapper;

    @Resource
    private RedisTemplate<String, SeckillGoods> goodsRedisTemplate;

    private RBloomFilter<Long> bloomFilter;
    private RedisScript<String> insertGoodsRetailer;
    private RedisScript<SeckillGoods> getGoodsRetailer;
    private RedisScript<String> canalSyncGoodsRetailer;
    private Cache<Long, SeckillGoods> seckillGoodsCache;
    private RedisScript<String> batchInsertGoodsRetailer;
    private RedisScript<Boolean> decreaseGoodsRetailerStorage;
    private RedisScript<String> batchRollBackGoodsRetailerStorage;

    @PostConstruct
    void init() {
        bloomFilter = redissonClient.getBloomFilter("orderGoodsList");
        bloomFilter.tryInit(10000L, 0.01);
        batchInsertGoodsRetailer = RedisScript.of(new ClassPathResource("lua/batchInsertGoodsRetailerScript.lua"), String.class);
        insertGoodsRetailer = RedisScript.of(new ClassPathResource("lua/insertGoodsRetailerScript.lua"), String.class);
        getGoodsRetailer = RedisScript.of(new ClassPathResource("lua/getGoodsRetailerScript.lua"), SeckillGoods.class);
        decreaseGoodsRetailerStorage = RedisScript.of(new ClassPathResource("lua/decreaseGoodsRetailerStorage.lua"), Boolean.class);
        batchRollBackGoodsRetailerStorage = RedisScript.of(new ClassPathResource("lua/batchRollBackGoodsRetailerStorage.lua"), String.class);
        canalSyncGoodsRetailer = RedisScript.of(new ClassPathResource("lua/canalSyncGoodsRetailerScript.lua"), String.class);
        seckillGoodsCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(300)
                .build();
    }

    public SeckillGoods get(Long retailerId, Long goodsId) {
        SeckillGoods seckillGoods = seckillGoodsCache.getIfPresent(goodsId);
        if (seckillGoods != null) return seckillGoods;

        if (!bloomFilter.contains(goodsId)) return null;

        seckillGoods = goodsRedisTemplate.execute(getGoodsRetailer, Collections.singletonList(REDIS_GOODS_RETAILER_KEY_PREFIX), retailerId, goodsId);
        if (seckillGoods != null) {
            set(seckillGoods, false);
            return seckillGoods;
        }

        seckillGoods = seckillGoodsMapper.selectOne(Wrappers.<SeckillGoods>lambdaQuery().eq(SeckillGoods::getRetailerId, retailerId).eq(SeckillGoods::getGoodsId, goodsId));
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
            goodsRedisTemplate.execute(insertGoodsRetailer, Collections.singletonList(REDIS_GOODS_RETAILER_KEY_PREFIX), seckillGoods);
    }

    public List<SeckillGoods> insertBatch(List<SeckillGoods> seckillGoods) {
        String arrayString = goodsRedisTemplate.execute(batchInsertGoodsRetailer, Collections.singletonList(REDIS_GOODS_RETAILER_KEY_PREFIX),
                seckillGoods.toArray());
        return arrayString.equals("{}") ? new ArrayList<>() : JSONArray.parseArray(arrayString, SeckillGoods.class);
    }

    public List<OrderVO> batchRollBackStorage(List<OrderVO> orderVOList) {
        String arrayString = goodsRedisTemplate.execute(batchRollBackGoodsRetailerStorage, Collections.singletonList(REDIS_GOODS_RETAILER_KEY_PREFIX), orderVOList.toArray());
        return arrayString.equals("{}") ? new ArrayList<>() : JSONArray.parseArray(arrayString, OrderVO.class);
    }

    public boolean decreaseStorage(Long retailerId, Long goodsId, int count) {
        return Boolean.TRUE.equals(goodsRedisTemplate.execute(decreaseGoodsRetailerStorage, Collections.singletonList(REDIS_GOODS_RETAILER_KEY_PREFIX), retailerId, goodsId, count));
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        goodsRedisTemplate.execute(canalSyncGoodsRetailer, Collections.singletonList(REDIS_GOODS_RETAILER_KEY_PREFIX),
                redisCommandList.toArray());
    }
}
