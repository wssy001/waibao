package com.waibao.seckill.service.cache;

import com.alibaba.fastjson.JSONArray;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.HashOperations;
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
 * PurchasedUserCacheService
 *
 * @author alexpetertyler
 * @since 2022-02-18
 */
@Service
@RequiredArgsConstructor
public class PurchasedUserCacheService {
    public static final String REDIS_PURCHASED_USER_KEY = "purchased-user-";

    @Resource
    private RedisTemplate<String, Integer> userRedisTemplate;

    private RedisScript<Boolean> reachLimit;
    private RedisScript<String> decreasePurchased;
    private RedisScript<Boolean> increasePurchasedCount;
    private Cache<Long, Long> purchasedUserCache;
    private HashOperations<String, Long, Long> hashOperations;

    @PostConstruct

    public void init() {
        reachLimit = RedisScript.of(new ClassPathResource("lua/reachLimitScript.lua"), Boolean.class);
        decreasePurchased = RedisScript.of(new ClassPathResource("lua/batchDecreasePurchasedScript.lua"), String.class);
        increasePurchasedCount = RedisScript.of(new ClassPathResource("lua/increasePurchasedCountScript.lua"), Boolean.class);
        purchasedUserCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();

        hashOperations = userRedisTemplate.opsForHash();
    }

    public Boolean increase(Long goodsId, Long userId, int count, int limit) {
        Boolean execute = userRedisTemplate.execute(increasePurchasedCount, Collections.singletonList(REDIS_PURCHASED_USER_KEY + goodsId), userId, count, limit);
        if (Boolean.TRUE.equals(execute)) {
            Long current = hashOperations.get(REDIS_PURCHASED_USER_KEY + goodsId, userId);
            purchasedUserCache.put(userId, current);
            return true;
        }
        return false;
    }

    public List<OrderVO> decreaseBatch(List<OrderVO> orderVOList) {
        String jsonArray = userRedisTemplate.execute(decreasePurchased, Collections.singletonList(REDIS_PURCHASED_USER_KEY), orderVOList.toArray());
        List<OrderVO> undoList = jsonArray.equals("{}") ? new ArrayList<>() : JSONArray.parseArray(jsonArray, OrderVO.class);
        orderVOList.parallelStream()
                .forEach(orderVO -> {
                    if (undoList.contains(orderVO)) {
                        purchasedUserCache.put(orderVO.getUserId(), 0L);
                    } else {
                        purchasedUserCache.asMap()
                                .computeIfPresent(orderVO.getUserId(), (k, v) -> v -= orderVO.getCount());
                    }
                });

        return undoList;
    }

    public boolean reachLimit(Long goodsId, Long userId, int limit) {
        Long count = purchasedUserCache.getIfPresent(userId);
        if (count != null) return count >= limit;

        Boolean result = userRedisTemplate.execute(reachLimit, Collections.singletonList(REDIS_PURCHASED_USER_KEY + goodsId), userId, limit);
        return Boolean.TRUE.equals(result);
    }
}
