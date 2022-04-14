package com.waibao.user.service.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.waibao.user.entity.UserExtra;
import com.waibao.user.mapper.UserExtraMapper;
import com.waibao.util.async.AsyncService;
import lombok.RequiredArgsConstructor;
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
 * UserExtraCacheService
 *
 * @author alexpetertyler
 * @since 2022/3/14
 */
@Service
@RequiredArgsConstructor
public class UserExtraCacheService {
    public static final String REDIS_USER_EXTRA_KEY_PREFIX = "user-extra-";

    private final AsyncService asyncService;
    private final UserExtraMapper userExtraMapper;

    @Resource
    private RedisTemplate<String, UserExtra> userExtraRedisTemplate;

    private BloomFilter<Long> bloomFilter;
    private Cache<Long, UserExtra> userExtraCache;
    private RedisScript<String> getUserExtra;
    private RedisScript<String> insertUserExtra;
    private RedisScript<String> batchGetUserExtra;
    private RedisScript<String> batchInsertUserExtra;

    @PostConstruct
    public void init() {
        bloomFilter = BloomFilter.create(Funnels.longFunnel(), 100000L, 0.001);
        getUserExtra = RedisScript.of(new ClassPathResource("lua/getUserExtraScript.lua"), String.class);
        insertUserExtra = RedisScript.of(new ClassPathResource("lua/insertUserExtraScript.lua"), String.class);
        batchGetUserExtra = RedisScript.of(new ClassPathResource("lua/batchGetUserExtraScript.lua"), String.class);
        batchInsertUserExtra = RedisScript.of(new ClassPathResource("lua/batchInsertUserExtraScript.lua"), String.class);

        userExtraCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(300)
                .build();
    }

    public UserExtra get(Long userId) {
        UserExtra userExtra = userExtraCache.getIfPresent(userId);
        if (userExtra != null) return userExtra;

        String execute = userExtraRedisTemplate.execute(getUserExtra, Collections.singletonList(REDIS_USER_EXTRA_KEY_PREFIX), userId + "");
        if (!"{}".equals(execute)) userExtra = JSON.parseObject(execute, UserExtra.class);
        if (userExtra != null) {
            set(userExtra, false);
            return userExtra;
        }

        if (!bloomFilter.mightContain(userId)) return null;

        userExtra = userExtraMapper.selectOne(Wrappers.<UserExtra>lambdaQuery().eq(UserExtra::getUserId, userId));
        if (userExtra != null) set(userExtra);

        return userExtra;
    }

    public List<UserExtra> get(List<Long> userIdList) {
//        Map<Long, UserExtra> allPresent = userExtraCache.getAllPresent(userIdList);
//        userIdList.removeAll(allPresent.keySet());
//        if (userIdList.isEmpty()) return new ArrayList<>(allPresent.values());

        List<String> collect = userIdList.parallelStream()
                .map(userId -> userId + "")
                .collect(Collectors.toList());
        String jsonArray = userExtraRedisTemplate.execute(batchGetUserExtra, Collections.singletonList(REDIS_USER_EXTRA_KEY_PREFIX), JSONArray.toJSONString(collect));
        return "{}".equals(jsonArray) ? new ArrayList<>() : JSONArray.parseArray(jsonArray, UserExtra.class);
    }

    public void set(UserExtra userExtra) {
        set(userExtra, true);
    }

    public void set(UserExtra userExtra, boolean updateRedis) {
        Long userId = userExtra.getUserId();
        userExtraCache.put(userId, userExtra);
        bloomFilter.put(userId);
        if (updateRedis)
            userExtraRedisTemplate.execute(insertUserExtra, Collections.singletonList(REDIS_USER_EXTRA_KEY_PREFIX), JSON.toJSONString(userExtra));
    }

    public void insertBatch(List<UserExtra> userExtraList) {
        asyncService.basicTask(() -> {
            Map<Long, UserExtra> collect = userExtraList.parallelStream()
                    .peek(userExtra -> bloomFilter.put(userExtra.getUserId()))
                    .collect(Collectors.toMap(UserExtra::getUserId, Function.identity()));
            userExtraCache.asMap()
                    .putAll(collect);
        });
        asyncService.basicTask(() -> userExtraRedisTemplate.execute(batchInsertUserExtra, Collections.singletonList(REDIS_USER_EXTRA_KEY_PREFIX), JSONArray.toJSONString(userExtraList)));
    }
}
