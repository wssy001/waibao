package com.waibao.user.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.waibao.user.entity.User;
import com.waibao.user.mapper.UserMapper;
import com.waibao.util.async.AsyncService;
import lombok.RequiredArgsConstructor;
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
    private final AsyncService asyncService;

    @Resource
    private RedisTemplate<String, User> userRedisTemplate;

    private Cache<Long, User> userCache;
    private BloomFilter<Long> bloomFilter;
    private RedisScript<User> getUser;
    private RedisScript<String> insertUser;
    private RedisScript<String> batchInsertUser;

    @PostConstruct
    public void init() {
        getUser = RedisScript.of(new ClassPathResource("lua/getUserScript.lua"), User.class);
        bloomFilter = BloomFilter.create(Funnels.longFunnel(), 100000L, 0.001);
        insertUser = RedisScript.of(new ClassPathResource("lua/insertUserScript.lua"), String.class);
        batchInsertUser = RedisScript.of(new ClassPathResource("lua/batchInsertUserScript.lua"), String.class);

        userCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(300)
                .build();
    }

    public User get(Long userId) {
        if (!bloomFilter.mightContain(userId)) return null;

        User user = userCache.getIfPresent(userId);
        if (user != null) return user;

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
        bloomFilter.put(userId);
        if (updateRedis)
            userRedisTemplate.execute(insertUser, Collections.singletonList(REDIS_USER_KEY_PREFIX), user);
    }

    public boolean checkUser(Long userId) {
        return get(userId) != null;
    }

    public void insertBatch(List<User> userList) {
        asyncService.basicTask(() -> userList.parallelStream().forEach(user -> userCache.put(user.getId(), user)));
        asyncService.basicTask(() -> userList.parallelStream().forEach(user -> bloomFilter.put(user.getId())));
        asyncService.basicTask(() -> userRedisTemplate.execute(batchInsertUser, Collections.singletonList(REDIS_USER_KEY_PREFIX), userList.toArray()));
    }

}
