package com.waibao.payment.service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;

/**
 * @Author: wwj
 * @Date: 2022/3/10
 */
@Service
@RequiredArgsConstructor
public class LogPaymentCacheService {
    public static final String REDIS_LOG_PAYMENT_KEY_PREFIX = "log-payment-";

    @Resource
    private RedisTemplate<String, String> logPaymentRedisTemplate;

    private DefaultRedisScript<Boolean> checkPaymentOperation;

    @PostConstruct
    void init() {
        String checkOperationScript = "local key = KEYS[1]\n" +
                "local count = tonumber(redis.call('LREM', key, 0, ARGV[1] .. '-' .. ARGV[2]))\n" +
                "if count > 0 then\n" +
                "    redis.call('LPUSH', key, ARGV[1] .. '-' .. ARGV[2])\n" +
                "    return true\n" +
                "else\n" +
                "    return false\n" +
                "end";
        checkPaymentOperation = new DefaultRedisScript<>(checkOperationScript, Boolean.class);
    }

    public boolean hasConsumeTags(Long userId,Long payId,String operation){
        return Boolean.TRUE.equals( logPaymentRedisTemplate.execute(checkPaymentOperation, Collections.singletonList(REDIS_LOG_PAYMENT_KEY_PREFIX+payId),userId,operation));
    }
}
