package com.waibao.order.service.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.waibao.order.entity.OrderUser;
import com.waibao.util.base.RedisCommand;
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
 * OrderUserCacheService
 *
 * @author alexpetertyler
 * @since 2022/2/28
 */
@Service
@RequiredArgsConstructor
public class OrderUserCacheService {
    public static final String REDIS_ORDER_USER_KEY_PREFIX = "order-user-";

    @Resource
    private RedisTemplate<String, String> orderUserRedisTemplate;

    private RedisScript<String> canalSync;
    private RedisScript<String> getOrderUser;
    private RedisScript<String> batchGetOrderUser;

    @PostConstruct
    void init() {
        getOrderUser = RedisScript.of(new ClassPathResource("lua/getOrderUserScript.lua"), String.class);
        canalSync = RedisScript.of(new ClassPathResource("lua/canalSyncOrderUserScript.lua"), String.class);
        batchGetOrderUser = RedisScript.of(new ClassPathResource("lua/batchGetOrderUserScript.lua"), String.class);
    }

    public OrderUser get(Long userId, String orderId) {
        String execute = orderUserRedisTemplate.execute(getOrderUser, Collections.singletonList(REDIS_ORDER_USER_KEY_PREFIX), userId + "", orderId);
        return JSON.parseObject(execute, OrderUser.class);
    }

    public List<OrderUser> getBatch(List<OrderVO> orderVOList) {
        String execute = orderUserRedisTemplate.execute(batchGetOrderUser, Collections.singletonList(REDIS_ORDER_USER_KEY_PREFIX), JSONArray.toJSONString(orderVOList));
        if ("{}".equals(execute)) return new ArrayList<>();
        return JSONArray.parseArray(execute, OrderUser.class);
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        orderUserRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_ORDER_USER_KEY_PREFIX), redisCommandList.toArray());
    }
}
