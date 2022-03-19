package com.waibao.seckill.service.cache;

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

    private final AsyncService asyncService;
    private final SeckillGoodsMapper seckillGoodsMapper;

    @Resource
    private RedisTemplate<String, SeckillGoods> goodsRedisTemplate;

    private RedisScript<String> canalSync;
    private BloomFilter<Long> bloomFilter;
    private RedisScript<String> batchInsertGoods;
    private RedisScript<String> insertSeckillGoods;
    private Cache<Long, Boolean> seckillFinishCache;
    private RedisScript<String> batchRollBackStorage;
    private RedisScript<SeckillGoods> getSeckillGoods;
    private RedisScript<Boolean> decreaseGoodsStorage;
    private Cache<Long, SeckillGoods> seckillGoodsCache;

    @PostConstruct
    void init() {
        bloomFilter = BloomFilter.create(Funnels.longFunnel(), 10000L, 0.001);
        batchInsertGoods = RedisScript.of(new ClassPathResource("lua/batchInsertGoodsScript.lua"), String.class);
        insertSeckillGoods = RedisScript.of(new ClassPathResource("lua/insertGoodsScript.lua"), String.class);
        getSeckillGoods = RedisScript.of(new ClassPathResource("lua/getGoodsScript.lua"), SeckillGoods.class);
        decreaseGoodsStorage = RedisScript.of(new ClassPathResource("lua/decreaseGoodsStorage.lua"), Boolean.class);
        batchRollBackStorage = RedisScript.of(new ClassPathResource("lua/batchRollBackGoodsStorage.lua"), String.class);
        canalSync = RedisScript.of(new ClassPathResource("lua/canalSyncGoodsScript.lua"), String.class);
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
        if (!bloomFilter.mightContain(goodsId)) return null;

        SeckillGoods seckillGoods = seckillGoodsCache.getIfPresent(goodsId);
        if (seckillGoods != null) return seckillGoods;

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
        bloomFilter.put(goodsId);
        if (updateRedis)
            goodsRedisTemplate.execute(insertSeckillGoods, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX), seckillGoods);
    }

    public void insertBatch(List<SeckillGoods> seckillGoodsList) {
        asyncService.basicTask(() -> seckillGoodsList.parallelStream().forEach(seckillGoods -> seckillGoodsCache.put(seckillGoods.getGoodsId(), seckillGoods)));
        asyncService.basicTask(() -> seckillGoodsList.parallelStream().forEach(seckillGoods -> bloomFilter.put(seckillGoods.getGoodsId())));
        asyncService.basicTask(() -> goodsRedisTemplate.execute(batchInsertGoods, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX), seckillGoodsList.toArray()));
    }

    public List<OrderVO> batchRollBackStorage(List<OrderVO> orderVOList) {
        String arrayString = goodsRedisTemplate.execute(batchRollBackStorage, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX), orderVOList.toArray());
        return arrayString.equals("{}") ? new ArrayList<>() : JSONArray.parseArray(arrayString, OrderVO.class);
    }

    public boolean decreaseStorage(Long goodsId, int count) {
        return Boolean.TRUE.equals(goodsRedisTemplate.execute(decreaseGoodsStorage, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX), goodsId, count));
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        asyncService.basicTask(() -> goodsRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX), redisCommandList.toArray()));
    }
}
