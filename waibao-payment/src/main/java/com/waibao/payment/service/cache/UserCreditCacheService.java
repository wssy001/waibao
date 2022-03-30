package com.waibao.payment.service.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.waibao.payment.entity.UserCredit;
import com.waibao.payment.service.db.UserCreditService;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.TimeUnit;
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

    private final UserCreditService userCreditService;

    @Resource
    private RedisTemplate<String, UserCredit> userCreditRedisTemplate;

    private Cache<Long, UserCredit> userCreditCache;
    private ValueOperations<String, UserCredit> valueOperations;

    @PostConstruct
    public void init() {
        valueOperations = userCreditRedisTemplate.opsForValue();
        userCreditCache = Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    public UserCredit get(Long userId) {
        UserCredit userCredit = userCreditCache.getIfPresent(userId);
        if (userCredit == null) {
            userCredit = valueOperations.get(REDIS_USER_CREDIT_KEY_PREFIX + userId);
        } else {
            return userCredit;
        }

        if (userCredit == null) {
            userCredit = userCreditService.getById(userId);
        } else {
            userCreditCache.put(userCredit.getUserId(), userCredit);
            return userCredit;
        }

        if (userCredit == null) return null;

        set(userCredit);
        return userCredit;
    }

    public void set(UserCredit userCredit) {
        userCreditCache.put(userCredit.getUserId(), userCredit);
        valueOperations.set(REDIS_USER_CREDIT_KEY_PREFIX + userCredit.getUserId(), userCredit);
    }

    public <T> List<T> batchDecreaseUserCredit(List<OrderVO> orderVOList, Class<T> clazz) {
        return orderVOList.parallelStream()
                .map(orderVO -> (JSONObject) JSON.toJSON(orderVO))
                .peek(jsonObject -> {
                    if (jsonObject.getString("status").equals("用户账户不存在")) return;
                    Long userId = jsonObject.getLong("userId");
                    BigDecimal orderPrice = jsonObject.getBigDecimal("orderPrice");
                    UserCredit userCredit = userCreditCache.asMap()
                            .computeIfPresent(userId, (k, v) -> {
                                BigDecimal money = v.getMoney();
                                jsonObject.put("oldMoney", money);
                                if (money.compareTo(orderPrice) < 0) return null;
                                v.setMoney(money.min(orderPrice));
                                jsonObject.put("money", v.getMoney());
                                return v;
                            });
                    if (userCredit == null) {
                        jsonObject.put("operation", "支付失败，余额不足");
                        jsonObject.put("status", "支付失败，余额不足");
                    } else {
                        jsonObject.put("paid", true);
                        jsonObject.put("status", "支付成功");
                        jsonObject.put("operation", "支付成功");
                    }
                })
                .map(jsonObject -> jsonObject.toJavaObject(clazz))
                .collect(Collectors.toList());
    }
}
