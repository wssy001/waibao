package com.waibao.seckill.service.cache;

import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.mapper.SeckillGoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.concurrent.Future;

/**
 * SeckillGoodsCacheService
 *
 * @author alexpetertyler
 * @since 2022-02-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillGoodsStorageCacheService {
    public static final String REDIS_SECKILL_GOODS_KEY_PREFIX = "seckill-goods-storage-";

    private final SeckillGoodsMapper seckillGoodsMapper;
    private final SeckillGoodsCacheService seckillGoodsCacheService;

    @Resource
    private RedisTemplate<String, Integer> storageRedisTemplate;

    private ValueOperations<String, Integer> valueOperations;
    private DefaultRedisScript<Boolean> increaseStorage;
    private DefaultRedisScript<Boolean> decreaseStorage;

    @PostConstruct
    public void init() {
        String increaseScript = "local key = KEYS[1]\n" +
                "local count = tonumber(ARGV[1])\n" +
                "local limit = tonumber(ARGV[2])\n" +
                "redis.call('INCRBY', key, count)\n" +
                "local storage = tonumber(redis.call('GET', key))\n" +
                "if (storage > limit) then\n" +
                "    redis.call('DECRBY', KEYS[1], count)\n" +
                "    return false\n" +
                "else\n" +
                "    return true\n" +
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
        valueOperations = storageRedisTemplate.opsForValue();
        increaseStorage = new DefaultRedisScript<>(increaseScript, Boolean.class);
        decreaseStorage = new DefaultRedisScript<>(decreaseScript, Boolean.class);
    }

    public int get(Long goodsId) {
        Integer storage = valueOperations.get(REDIS_SECKILL_GOODS_KEY_PREFIX + goodsId);
        if (storage == null || storage < 1) return 0;
        return storage;
    }

    public Boolean increaseStorage(Long goodsId, int count) {
        SeckillGoods seckillGoods = seckillGoodsCacheService.get(goodsId);
        return storageRedisTemplate.execute(increaseStorage, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX + goodsId), count, seckillGoods.getStorage());
    }

    @Async
    public Future<Boolean> increaseStorageAsync(Long goodsId, int count) {
        return new AsyncResult<>(increaseStorage(goodsId, count));
    }

    public Boolean decreaseStorage(Long goodsId, int count) {
        return storageRedisTemplate.execute(decreaseStorage, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX + goodsId), count);
    }

    @Async
    public Future<Boolean> decreaseStorageAsync(Long goodsId, int count) {
        return new AsyncResult<>(decreaseStorage(goodsId, count));
    }

    public void set(SeckillGoods seckillGoods) {
        valueOperations.set(REDIS_SECKILL_GOODS_KEY_PREFIX + seckillGoods.getGoodsId(), seckillGoods.getStorage());
    }

    @Scheduled(fixedDelay = 5 * 60 * 1000L)
    public void updateCache() {
        seckillGoodsMapper.selectList(null)
                .parallelStream()
                .forEach(this::set);
    }
}
