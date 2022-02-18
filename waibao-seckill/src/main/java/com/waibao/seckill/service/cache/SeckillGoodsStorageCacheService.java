package com.waibao.seckill.service.cache;

import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.mapper.SeckillGoodsMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;

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

    @Resource
    private ValueOperations<String, Integer> valueOperations;

    private DefaultRedisScript<Integer> decreaseStorage;

    @PostConstruct
    public void init() {
        String luaScript = "local key=KEYS[1] " +
                "local subNum = tonumber(ARGV[1]) " +
                "local surplusStock = tonumber(redis.call('GET',key)) " +
                "if (surplusStock <= 0) then return 0 " +
                "elseif (subNum > surplusStock) then return 1 " +
                "else redis.call('DECRBY', KEYS[1], subNum) return 2 end";
        decreaseStorage = new DefaultRedisScript<>(luaScript, Integer.class);
    }

    public int get(Long goodsId) {
        Integer storage = valueOperations.get(REDIS_SECKILL_GOODS_KEY_PREFIX + goodsId);
        if (storage == null || storage < 1) return 0;
        return storage;
    }

    // 已经减库存 返回2
    public boolean decreaseStorage(Long goodsId, int count) {
        Integer result = valueOperations.getOperations()
                .execute(decreaseStorage, Collections.singletonList(REDIS_SECKILL_GOODS_KEY_PREFIX + goodsId), count);
        if (result == null) return false;
        return result == 2;
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
