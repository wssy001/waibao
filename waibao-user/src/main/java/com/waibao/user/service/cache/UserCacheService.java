package com.waibao.user.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.waibao.user.entity.User;
import com.waibao.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * UserService
 *
 * @author alexpetertyler
 * @since 2022-02-15
 */
@Service
@RequiredArgsConstructor
public class UserCacheService {
    public static final String REDIS_USER_KEY_PREFIX = "user-";

    private final UserMapper userMapper;
    private final RedissonClient redissonClient;

    @Resource
    private RedisTemplate<String, User> userRedisTemplate;

    private Cache<Long, User> userCache;
    private RBloomFilter<Long> bloomFilter;
    private RedisScript<User> getUser;
    private RedisScript<String> insertUser;
    private RedisScript<String> batchInsertUser;

    @PostConstruct
    public void init() {
        bloomFilter = redissonClient.getBloomFilter("userList");
        bloomFilter.tryInit(1000000L, 0.03);
        getUser = RedisScript.of(new ClassPathResource("lua/getUserScript.lua"), User.class);
        insertUser = RedisScript.of(new ClassPathResource("lua/insertUserScript.lua"), String.class);
        batchInsertUser = RedisScript.of(new ClassPathResource("lua/batchInsertUserScript.lua"), String.class);

        userCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(300)
                .build();
    }

    public User get(Long userId) {
        User user = userCache.getIfPresent(userId);
        if (user != null) return user;

        if (!bloomFilter.contains(userId)) return null;

        user = userRedisTemplate.execute(getUser, Collections.singletonList(REDIS_USER_KEY_PREFIX), userId);
        if (user != null) {
            set(user, false);
            return user;
        }

        user = userMapper.selectById(userId);
        if (user != null) set(user);

        return user;
    }

    public void set(User user) {
        set(user, true);
    }

    public void set(User user, boolean updateRedis) {
        Long userId = user.getId();
        userCache.put(userId, user);
        bloomFilter.add(userId);
        if (updateRedis)
            userRedisTemplate.execute(insertUser, Collections.singletonList(REDIS_USER_KEY_PREFIX), user);
    }

    public boolean checkUser(Long userId) {
        return get(userId) != null;
    }

    public void insertBatch(List<User> userList) {
        userRedisTemplate.execute(batchInsertUser, Collections.singletonList(REDIS_USER_KEY_PREFIX),
                userList.toArray());
    }

}
