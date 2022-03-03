package com.waibao.seckill.service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;

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

    private ValueOperations<String, Integer> valueOperations;
    private DefaultRedisScript<Boolean> increaseCount;
    private DefaultRedisScript<Boolean> reachLimit;

    @PostConstruct
    public void init() {
        String luaScript1 = "local key = KEYS[1] \n" +
                "local count = tonumber(ARGV[1]) \n" +
                "local limit = tonumber(ARGV[2]) \n" +
                "redis.call('INCRBY',key,count)\n" +
                "local currentCount = tonumber(redis.call('GET',key)) \n" +
                "if (currentCount > limit) then\n" +
                "redis.call('DECRBY',key,count)\n" +
                "return false\n" +
                "else\n" +
                "return true\n" +
                "end";
        String luaScript2 = "local key = KEYS[1]\n" +
                "local count = tonumber(ARGV[1])\n" +
                "local currentCount = tonumber(redis.call('GET',key))\n" +
                "if (currentCount == nil) then return false\n" +
                "end\n" +
                "return(currentCount >= limit)";
        valueOperations = userRedisTemplate.opsForValue();
        increaseCount = new DefaultRedisScript<>(luaScript1, Boolean.class);
        reachLimit = new DefaultRedisScript<>(luaScript2, Boolean.class);
    }

    public Boolean increase(Long userId, int count, int limit) {
        return userRedisTemplate.execute(increaseCount, Collections.singletonList(REDIS_PURCHASED_USER_KEY + userId), count, limit);
    }

    public void decrease(Long userId, int count) {
        valueOperations.decrement(REDIS_PURCHASED_USER_KEY + userId, count);
    }

    public boolean reachLimit(Long userId, int limit) {
        Boolean result = userRedisTemplate.execute(reachLimit, Collections.singletonList(REDIS_PURCHASED_USER_KEY + userId), limit);
        return Boolean.TRUE.equals(result);
    }
}
