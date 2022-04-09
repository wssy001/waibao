package com.waibao.payment.service.cache;

import com.alibaba.fastjson.JSONArray;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.waibao.util.base.RedisCommand;
import com.waibao.util.vo.payment.PaymentVO;
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
import java.util.stream.Collectors;

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
    private RedisScript<String> batchCheckPaymentOperation;

    @PostConstruct
    void init() {
        canalSync = RedisScript.of(new ClassPathResource("lua/canalSyncLogPaymentScript.lua"), String.class);
        bloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 40000L, 0.001);
        checkPaymentOperation = RedisScript.of(new ClassPathResource("lua/checkLogPaymentOperationScript.lua"), Boolean.class);
        batchCheckPaymentOperation = RedisScript.of(new ClassPathResource("lua/batchCheckLogPaymentOperationScript.lua"), String.class);
    }

    public void putToBloomFilter(String payId, String operation) {
        bloomFilter.put(payId + operation);
    }

    public boolean hasConsumeTags(Long userId, String payId, String operation) {
        if (!bloomFilter.mightContain(payId + operation)) return false;
        return Boolean.TRUE.equals(logPaymentRedisTemplate.execute(checkPaymentOperation, Collections.singletonList(REDIS_LOG_PAYMENT_KEY_PREFIX + payId), userId + "", operation));
    }

    public List<PaymentVO> batchCheckNotConsumeTags(List<PaymentVO> paymentVOList, String operation) {
        List<PaymentVO> temp = paymentVOList.parallelStream()
                .filter(paymentVO -> !bloomFilter.mightContain(paymentVO.getPayId() + operation))
                .collect(Collectors.toList());
        if (temp.size() == paymentVOList.size()) return temp;

        String execute = logPaymentRedisTemplate.execute(batchCheckPaymentOperation, Collections.singletonList(REDIS_LOG_PAYMENT_KEY_PREFIX), JSONArray.toJSONString(paymentVOList), operation);
        if (!"{}".equals(execute)) temp.addAll(JSONArray.parseArray(execute, PaymentVO.class));
        return temp;
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        logPaymentRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_LOG_PAYMENT_KEY_PREFIX), JSONArray.toJSONString(redisCommandList));
    }
}
