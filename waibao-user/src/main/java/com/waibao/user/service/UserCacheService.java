package com.waibao.user.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.user.entity.User;
import com.waibao.user.service.db.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

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

    @Resource
    private ValueOperations<Long, User> valueOperations;

    public User get(Long userNo) {
        User user = valueOperations.get(userNo);
        if (user == null) {
            user = userService.getOne(Wrappers.<User>lambdaQuery().eq(User::getUserNo, userNo));
            if (user == null) user = new User();
            valueOperations.set(userNo, user, 15 * 60, TimeUnit.SECONDS);
        }

        return user;
    }

    public void set(User user) {
        valueOperations.set(user.getUserNo(), user);
    }
}
