package com.waibao.payment.service.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.waibao.payment.entity.Payment;
import com.waibao.payment.mapper.PaymentMapper;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @Author: wwj
 * @Date: 2022/3/5
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentCacheService {
    public final static String REDIS_PAYMENT_KEY_PREFIX = "payment-";

    private final PaymentMapper paymentMapper;

    @Resource
    private RedisTemplate<String, String> paymentRedisTemplate;

    private RedisScript<String> canalSync;
    private RedisScript<String> getPayment;
    private BloomFilter<String> bloomFilter;
    private RedisScript<String> insertPayment;
    private RedisScript<String> batchGetPayment;
    private Cache<String, Payment> paymentCache;
    private RedisScript<String> batchInsertPayment;

    @PostConstruct
    public void init() {
        getPayment = RedisScript.of(new ClassPathResource("lua/getPaymentScript.lua"), String.class);
        canalSync = RedisScript.of(new ClassPathResource("lua/canalSyncPaymentScript.lua"), String.class);
        insertPayment = RedisScript.of(new ClassPathResource("lua/insertPaymentScript.lua"), String.class);
        batchGetPayment = RedisScript.of(new ClassPathResource("lua/batchGetPaymentScript.lua"), String.class);
        batchInsertPayment = RedisScript.of(new ClassPathResource("lua/batchInsertPaymentScript.lua"), String.class);

        bloomFilter = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8), 15000, 0.001);
        paymentCache = Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    public Payment get(String payId) {
        Payment payment = paymentCache.getIfPresent(payId);
        if (payment != null) return payment;

        String execute = paymentRedisTemplate.execute(getPayment, Collections.singletonList(REDIS_PAYMENT_KEY_PREFIX), payId);
        if (!"{}".equals(execute)) {
            payment = JSON.parseObject(execute, Payment.class);
            set(payment, false);
            return payment;
        }
        if (!bloomFilter.mightContain(payId)) return null;

        payment = paymentMapper.selectOne(Wrappers.<Payment>lambdaQuery().eq(Payment::getPayId, payId));
        if (payment != null) {
            set(payment);
        }

        return payment;
    }

    public List<Payment> batchGet(List<String> payIdList) {
        Map<String, Payment> allPresent = paymentCache.getAllPresent(payIdList);
        ArrayList<Payment> payments = new ArrayList<>(allPresent.values());
        if (payments.size() == payIdList.size()) return payments;

        payIdList = payIdList.parallelStream()
                .filter(payId -> !allPresent.containsKey(payId))
                .filter(bloomFilter::mightContain)
                .collect(Collectors.toList());

        String execute = paymentRedisTemplate.execute(batchGetPayment, Collections.singletonList(REDIS_PAYMENT_KEY_PREFIX), JSONArray.toJSONString(payIdList));
        if (!"{}".equals(execute)) payments.addAll(JSONArray.parseArray(execute, Payment.class));
        return payments;
    }

    public void set(Payment payment) {
        set(payment, true);
    }

    public void set(Payment payment, boolean updateRedis) {
        bloomFilter.put(payment.getPayId());
        paymentCache.put(payment.getPayId(), payment);
        if (updateRedis)
            paymentRedisTemplate.execute(insertPayment, Collections.singletonList(REDIS_PAYMENT_KEY_PREFIX), JSONArray.toJSONString(payment));
    }

    public void batchSet(List<Payment> paymentList) {
        Map<String, Payment> collect = paymentList.stream()
                .peek(payment -> bloomFilter.put(payment.getPayId()))
                .collect(Collectors.toMap(Payment::getPayId, Function.identity()));
        paymentCache.asMap()
                        .putAll(collect);

        paymentRedisTemplate.execute(batchInsertPayment, Collections.singletonList(REDIS_PAYMENT_KEY_PREFIX), JSONArray.toJSONString(paymentList));
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        paymentRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_PAYMENT_KEY_PREFIX), JSONArray.toJSONString(redisCommandList));
    }
}
