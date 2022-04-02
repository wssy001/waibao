package com.waibao.seckill.service.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.waibao.util.async.AsyncService;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.mapper.SeckillGoodsMapper;
import com.waibao.util.base.RedisCommand;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    private final AsyncService asyncService;
    private final SeckillGoodsMapper seckillGoodsMapper;

    @Resource
    private RedisTemplate<String, SeckillGoods> goodsRedisTemplate;

    private BloomFilter<Long> bloomFilter;
    private RedisScript<String> insertGoodsRetailer;
    private RedisScript<String> getGoodsRetailer;
    private RedisScript<String> canalSyncGoodsRetailer;
    private Cache<Long, SeckillGoods> seckillGoodsCache;
    private RedisScript<String> batchInsertGoodsRetailer;
    private RedisScript<Boolean> decreaseGoodsRetailerStorage;
    private RedisScript<String> batchRollBackGoodsRetailerStorage;

    @PostConstruct
    void init() {
        bloomFilter = BloomFilter.create(Funnels.longFunnel(), 10L, 0.1);
        getGoodsRetailer = RedisScript.of(new ClassPathResource("lua/getGoodsRetailerScript.lua"), String.class);
        insertGoodsRetailer = RedisScript.of(new ClassPathResource("lua/insertGoodsRetailerScript.lua"), String.class);
        canalSyncGoodsRetailer = RedisScript.of(new ClassPathResource("lua/canalSyncGoodsRetailerScript.lua"), String.class);
        batchInsertGoodsRetailer = RedisScript.of(new ClassPathResource("lua/batchInsertGoodsRetailerScript.lua"), String.class);
        decreaseGoodsRetailerStorage = RedisScript.of(new ClassPathResource("lua/decreaseGoodsRetailerStorage.lua"), Boolean.class);
        batchRollBackGoodsRetailerStorage = RedisScript.of(new ClassPathResource("lua/batchRollBackGoodsRetailerStorage.lua"), String.class);

        seckillGoodsCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(300)
                .build();
    }

    public SeckillGoods get(Long retailerId, Long goodsId) {
        SeckillGoods seckillGoods = seckillGoodsCache.getIfPresent(goodsId);
        if (seckillGoods != null) return seckillGoods;

        String execute = goodsRedisTemplate.execute(getGoodsRetailer, Collections.singletonList(REDIS_GOODS_RETAILER_KEY_PREFIX), retailerId + "", goodsId + "");
        if (!"{}".equals(execute)) seckillGoods = JSON.parseObject(execute, SeckillGoods.class);
        if (seckillGoods != null) {
            set(seckillGoods, false);
            return seckillGoods;
        }
        if (!bloomFilter.mightContain(goodsId)) return null;

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
        bloomFilter.put(goodsId);
        if (updateRedis)
            goodsRedisTemplate.execute(insertGoodsRetailer, Collections.singletonList(REDIS_GOODS_RETAILER_KEY_PREFIX), JSON.toJSONString(seckillGoods));
    }

    public void insertBatch(List<SeckillGoods> seckillGoodsList) {
        asyncService.basicTask(() -> seckillGoodsList.parallelStream().forEach(seckillGoods -> seckillGoodsCache.put(seckillGoods.getGoodsId(), seckillGoods)));
        asyncService.basicTask(() -> seckillGoodsList.parallelStream().forEach(seckillGoods -> bloomFilter.put(seckillGoods.getGoodsId())));
        asyncService.basicTask(() -> goodsRedisTemplate.execute(batchInsertGoodsRetailer, Collections.singletonList(REDIS_GOODS_RETAILER_KEY_PREFIX), JSONArray.toJSONString(seckillGoodsList)));
    }

    public List<OrderVO> batchRollBackStorage(List<OrderVO> orderVOList) {
        String arrayString = goodsRedisTemplate.execute(batchRollBackGoodsRetailerStorage, Collections.singletonList(REDIS_GOODS_RETAILER_KEY_PREFIX), JSONArray.toJSONString(orderVOList));
        return "{}".equals(arrayString) ? new ArrayList<>() : JSONArray.parseArray(arrayString, OrderVO.class);
    }

    public boolean decreaseStorage(Long retailerId, Long goodsId, int count) {
        return Boolean.TRUE.equals(goodsRedisTemplate.execute(decreaseGoodsRetailerStorage, Collections.singletonList(REDIS_GOODS_RETAILER_KEY_PREFIX), retailerId + "", goodsId + "", count + ""));
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        goodsRedisTemplate.execute(canalSyncGoodsRetailer, Collections.singletonList(REDIS_GOODS_RETAILER_KEY_PREFIX), JSONArray.toJSONString(redisCommandList));
    }
}
