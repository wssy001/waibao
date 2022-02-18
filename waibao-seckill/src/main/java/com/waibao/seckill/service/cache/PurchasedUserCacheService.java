package com.waibao.seckill.service.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * PurchasedUserCacheService
 *
 * @author alexpetertyler
 * @since 2022-02-18
 */
@Service
@RequiredArgsConstructor
public class PurchasedUserCacheService {
    public static final String REDIS_PURCHASED_USER_KEY = "purchased-user-";

    @Resource
    private ValueOperations<String, Integer> valueOperations;

    //todo
    public int increase(Long userId, int count,int limit) {

    }

    //todo
    public boolean reachLimit(Long userId,int limit){

    }
}
