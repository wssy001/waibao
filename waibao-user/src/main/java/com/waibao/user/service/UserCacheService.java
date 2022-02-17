package com.waibao.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.user.entity.User;
import com.waibao.user.service.db.UserService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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
    private final UserService userService;
    private final RedissonClient redissonClient;

    private RBloomFilter<Long> bloomFilter;

    @PostConstruct
    public void init() {
        bloomFilter = redissonClient.getBloomFilter("userList");
        bloomFilter.tryInit(1000000L, 0.03);
    }

    @Resource
    private ValueOperations<Long, User> valueOperations;

    public User get(Long userNo) {
        User user = valueOperations.get(userNo);
        if (user == null) {
            user = userService.getOne(Wrappers.<User>lambdaQuery().eq(User::getUserNo, userNo));
            if (user == null) user = new User();
            valueOperations.set(userNo, user, 15 * 60, TimeUnit.SECONDS);
        }

        if (user.getUserNo() != null) bloomFilter.add(userNo);
        return user;
    }

    public void set(User user) {
        valueOperations.set(user.getUserNo(), user);
    }

    public boolean checkUser(Long userNo) {
        if (!bloomFilter.contains(userNo)) return false;
        return get(userNo).getUserNo() != null;
    }
}
