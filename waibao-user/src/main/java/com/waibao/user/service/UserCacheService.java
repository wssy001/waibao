package com.waibao.user.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.waibao.user.entity.User;
import com.waibao.user.mapper.UserMapper;
import com.waibao.user.service.db.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * UserService
 *
 * @author alexpetertyler
 * @since 2022-02-15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserCacheService {
    public static final String REDIS_USER_KEY_PREFIX = "user-";

    private final UserMapper userMapper;
    private final UserService userService;
    private final RedissonClient redissonClient;

    @Resource
    private RedisTemplate<String, User> userRedisTemplate;

    private RBloomFilter<Long> bloomFilter;
    private AtomicLong atomicLong;
    private ValueOperations<String, User> valueOperations;

    @PostConstruct
    public void init() {
        bloomFilter = redissonClient.getBloomFilter("userList");
        bloomFilter.tryInit(1000000L, 0.03);
        Long count = userMapper.selectCount(null);
        atomicLong = new AtomicLong(count / 1000 + 1);
        valueOperations = userRedisTemplate.opsForValue();
    }

    public User get(Long userNo) {
        User user = valueOperations.get(REDIS_USER_KEY_PREFIX + userNo);
        if (user == null) {
            user = userService.getOne(Wrappers.<User>lambdaQuery().eq(User::getUserNo, userNo));
            if (user == null) user = new User();
            valueOperations.set(REDIS_USER_KEY_PREFIX + userNo, user, 15 * 60, TimeUnit.SECONDS);
        }

        if (user.getUserNo() != null) bloomFilter.add(userNo);
        return user;
    }

    public void set(User user) {
        valueOperations.set(REDIS_USER_KEY_PREFIX + user.getUserNo(), user);
        if (user.getUserNo() != null) bloomFilter.add(user.getUserNo());
    }

    public boolean checkUser(Long userNo) {
        if (!bloomFilter.contains(userNo)) return false;
        return get(userNo).getUserNo() != null;
    }

    @Scheduled(fixedDelay = 60 * 1000L)
    public void storeUser() {
        log.info("******UserCacheService：开始读取数据库放入缓存");
        long l = atomicLong.get();
        if (l > 0) {
            IPage<User> userPage = new Page<>(l, 1000);
            userPage = userMapper.selectPage(userPage, null);

            userPage.getRecords()
                    .parallelStream()
                    .forEach(this::set);
            atomicLong.getAndDecrement();
        }
        log.info("******UserCacheService：读取数据库放入缓存结束");
    }
}
