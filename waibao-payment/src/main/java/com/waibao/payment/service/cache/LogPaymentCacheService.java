package com.waibao.payment.service.cache;

import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

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

    private RedisScript<String> canalSync;
    private BloomFilter<String> bloomFilter;
    private RedisScript<Boolean> checkPaymentOperation;

    @PostConstruct
    void init() {
        canalSync = RedisScript.of(new ClassPathResource("lua/canalSyncLogPaymentScript.lua"), String.class);
        bloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 40000L, 0.001);
        checkPaymentOperation = RedisScript.of(new ClassPathResource("lua/checkLogPaymentOperationScript.lua"), Boolean.class);
    }

    public void putToBloomFilter(String payId, String operation) {
        bloomFilter.put(payId + operation);
    }

    public boolean hasConsumeTags(Long userId, String payId, String operation) {
        if (!bloomFilter.mightContain(payId + operation)) return false;
        return Boolean.TRUE.equals(logPaymentRedisTemplate.execute(checkPaymentOperation, Collections.singletonList(REDIS_LOG_PAYMENT_KEY_PREFIX + payId), userId, operation));
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        logPaymentRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_LOG_PAYMENT_KEY_PREFIX), redisCommandList.toArray());
    }
}
