package com.waibao.seckill.service.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.waibao.util.vo.order.OrderVO;
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
    private RedisTemplate<String, Integer> userRedisTemplate;

    private RedisScript<Boolean> reachLimit;
    private RedisScript<String> decreasePurchased;
    private RedisScript<Boolean> increasePurchasedCount;

    @PostConstruct
    public void init() {
        reachLimit = RedisScript.of(new ClassPathResource("lua/reachLimitScript.lua"), Boolean.class);
        decreasePurchased = RedisScript.of(new ClassPathResource("lua/decreasePurchasedScript.lua"), String.class);
        increasePurchasedCount = RedisScript.of(new ClassPathResource("lua/increasePurchasedCountScript.lua"), Boolean.class);
    }

    public Boolean increase(Long userId, int count, int limit) {
        return userRedisTemplate.execute(increasePurchasedCount, Collections.singletonList(REDIS_PURCHASED_USER_KEY + userId), count, limit);
    }

    public List<OrderVO> decreaseBatch(List<OrderVO> orderVOList) {
        String jsonArray = userRedisTemplate.execute(decreasePurchased, Collections.singletonList(REDIS_PURCHASED_USER_KEY), orderVOList.toArray());
        if (StrUtil.isBlank(jsonArray)) return new ArrayList<>();
        return JSONArray.parseArray(jsonArray, OrderVO.class);
    }

    public boolean reachLimit(Long userId, int limit) {
        Boolean result = userRedisTemplate.execute(reachLimit, Collections.singletonList(REDIS_PURCHASED_USER_KEY + userId), limit);
        return Boolean.TRUE.equals(result);
    }
}
