package com.waibao.payment.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.waibao.payment.entity.Payment;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @Author: wwj
 * @Date: 2022/3/5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCacheService {
    private final String REDIS_USER_CREDIT_KEY_PREFIX = "payment-";

    @Resource
    private RedisTemplate<String, Payment> paymentRedisTemplate;

    private RedisScript<String> canalSync;
    private Cache<String, Payment> paymentCache;
    private ValueOperations<String, Payment> valueOperations;

    @PostConstruct
    public void init() {
        valueOperations = paymentRedisTemplate.opsForValue();
        canalSync = RedisScript.of(new ClassPathResource("lua/canalSyncPaymentScript.lua"), String.class);

        paymentCache = Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    public List<Payment> batchGet(List<String> payIdList) {
        List<String> keyList = payIdList.parallelStream()
                .map(payId -> REDIS_USER_CREDIT_KEY_PREFIX + payId)
                .collect(Collectors.toList());
        return valueOperations.multiGet(payIdList);
    }

    public Payment get(String payId) {
        return valueOperations.get(REDIS_USER_CREDIT_KEY_PREFIX + payId);
    }

    public void set(Payment payment) {
        paymentCache.put(payment.getPayId(), payment);
        valueOperations.set(REDIS_USER_CREDIT_KEY_PREFIX + payment.getPayId(), payment);
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        paymentRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_USER_CREDIT_KEY_PREFIX), redisCommandList.toArray());
    }
}
