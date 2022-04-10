package com.waibao.payment.service.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.waibao.payment.entity.UserCredit;
import com.waibao.payment.mapper.UserCreditMapper;
import com.waibao.util.async.AsyncService;
import com.waibao.util.base.RedisCommand;
import com.waibao.util.vo.order.OrderVO;
import com.waibao.util.vo.payment.PaymentVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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
public class UserCreditCacheService {
    private final String REDIS_USER_CREDIT_KEY_PREFIX = "user-credit-";

    private final AsyncService asyncService;
    private final UserCreditMapper userCreditMapper;

    @Resource
    private RedisTemplate<String, String> userCreditRedisTemplate;

    private RedisScript<String> canalSync;
    private BloomFilter<Long> bloomFilter;
    private RedisScript<String> getUserCredit;
    private RedisScript<String> insertUserCredit;
    private RedisScript<String> batchGetUserCredit;
    private RedisScript<String> batchDecreaseCredit;
    private RedisScript<String> batchIncreaseCredit;
    private Cache<Long, UserCredit> userCreditCache;
    private RedisScript<String> batchInsertUserCredit;

    @PostConstruct
    public void init() {
        getUserCredit = RedisScript.of(new ClassPathResource("lua/getUserCreditScript.lua"), String.class);
        canalSync = RedisScript.of(new ClassPathResource("lua/canalSyncUserCreditScript.lua"), String.class);
        insertUserCredit = RedisScript.of(new ClassPathResource("lua/insertUserCreditScript.lua"), String.class);
        batchGetUserCredit = RedisScript.of(new ClassPathResource("lua/batchGetUserCreditScript.lua"), String.class);
        batchIncreaseCredit = RedisScript.of(new ClassPathResource("lua/batchIncreaseCreditScript.lua"), String.class);
        batchDecreaseCredit = RedisScript.of(new ClassPathResource("lua/batchDecreaseCreditScript.lua"), String.class);
        batchInsertUserCredit = RedisScript.of(new ClassPathResource("lua/batchInsertUserCreditScript.lua"), String.class);

        bloomFilter = BloomFilter.create(Funnels.longFunnel(), 15000, 0.001);
        userCreditCache = Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    public UserCredit get(Long userId) {
        UserCredit userCredit = userCreditCache.getIfPresent(userId);
        if (userCredit != null) return userCredit;

        String execute = userCreditRedisTemplate.execute(getUserCredit, Collections.singletonList(REDIS_USER_CREDIT_KEY_PREFIX), userId + "");
        if (!"{}".equals(execute)) {
            userCredit = JSON.parseObject(execute, UserCredit.class);
            set(userCredit, false);
            return userCredit;
        }
        if (!bloomFilter.mightContain(userId)) return null;

        userCredit = userCreditMapper.selectOne(Wrappers.<UserCredit>lambdaQuery().eq(UserCredit::getUserId, userId));
        if (userCredit != null) {
            set(userCredit);
        }

        return userCredit;
    }

    public List<UserCredit> batchGet(List<Long> userIdList) {
        Map<Long, UserCredit> allPresent = userCreditCache.getAllPresent(userIdList);
        ArrayList<UserCredit> userCredits = new ArrayList<>(allPresent.values());
        if (userCredits.size() == userIdList.size()) return userCredits;

        List<String> idList = userIdList.parallelStream()
                .filter(userId -> !allPresent.containsKey(userId))
                .filter(bloomFilter::mightContain)
                .map(id -> id + "")
                .collect(Collectors.toList());

        String execute = userCreditRedisTemplate.execute(batchGetUserCredit, Collections.singletonList(REDIS_USER_CREDIT_KEY_PREFIX), JSONArray.toJSONString(idList));
        if (!"{}".equals(execute)) userCredits.addAll(JSONArray.parseArray(execute, UserCredit.class));
        return userCredits;
    }

    public void set(UserCredit userCredit) {
        set(userCredit, true);
    }

    public void set(UserCredit userCredit, boolean updateRedis) {
        bloomFilter.put(userCredit.getUserId());
        userCreditCache.put(userCredit.getUserId(), userCredit);
        if (updateRedis)
            userCreditRedisTemplate.execute(insertUserCredit, Collections.singletonList(REDIS_USER_CREDIT_KEY_PREFIX), JSONArray.toJSONString(userCredit));
    }

    public void batchSet(List<UserCredit> userCreditList) {
        asyncService.basicTask(() -> {
            Map<Long, UserCredit> collect = userCreditList.stream()
                    .peek(userCredit -> bloomFilter.put(userCredit.getUserId()))
                    .collect(Collectors.toMap(UserCredit::getUserId, Function.identity()));
            userCreditCache.asMap()
                    .putAll(collect);
        });
        asyncService.basicTask(() -> userCreditRedisTemplate.execute(batchInsertUserCredit, Collections.singletonList(REDIS_USER_CREDIT_KEY_PREFIX), JSONArray.toJSONString(userCreditList)));
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        userCreditRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_USER_CREDIT_KEY_PREFIX), JSONArray.toJSONString(redisCommandList));
    }

    public List<JSONObject> batchDecreaseUserCredit(List<OrderVO> orderVOList) {
        String execute = userCreditRedisTemplate.execute(batchDecreaseCredit, Collections.singletonList(REDIS_USER_CREDIT_KEY_PREFIX), JSONArray.toJSONString(orderVOList));
        if ("{}".equals(execute)) return new ArrayList<>();
        return JSONArray.parseArray(execute, JSONObject.class);
    }

    public List<JSONObject> batchIncreaseUserCredit(List<PaymentVO> paymentVOList) {
        String execute = userCreditRedisTemplate.execute(batchIncreaseCredit, Collections.singletonList(REDIS_USER_CREDIT_KEY_PREFIX), JSONArray.toJSONString(paymentVOList));
        if ("{}".equals(execute)) return new ArrayList<>();
        return JSONArray.parseArray(execute, JSONObject.class);
    }
}
