package com.waibao.seckill.service.cache;

import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.mapper.SeckillGoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * SeckillGoodsCacheService
 *
 * @author alexpetertyler
 * @since 2022-02-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillGoodsRetailerCacheService {
    public static final String REDIS_SECKILL_GOODS_KEY_PREFIX = "seckill-goods-";

    private final SeckillGoodsMapper seckillGoodsMapper;

    @Resource
    private RedisTemplate<String, SeckillGoods> GoodsRetailerRedisTemplate;

    private ValueOperations<String, SeckillGoods> valueOperations;

    @PostConstruct
    void init() {
        valueOperations = GoodsRetailerRedisTemplate.opsForValue();
    }

    public SeckillGoods get(Long goodsId) {
        SeckillGoods seckillGoods = valueOperations.get(REDIS_SECKILL_GOODS_KEY_PREFIX + goodsId);
        if (seckillGoods == null) return new SeckillGoods();
        return seckillGoods;
    }

    public void set(SeckillGoods seckillGoods) {
        valueOperations.set(REDIS_SECKILL_GOODS_KEY_PREFIX + seckillGoods.getGoodsId(), seckillGoods);
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void updateCache() {
        seckillGoodsMapper.selectList(null)
                .parallelStream()
                .forEach(this::set);
    }
}
