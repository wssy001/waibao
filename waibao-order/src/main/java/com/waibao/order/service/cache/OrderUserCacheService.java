package com.waibao.order.service.cache;

import com.alibaba.fastjson.JSONArray;
import com.waibao.order.entity.OrderUser;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * OrderUserCacheService
 *
 * @author alexpetertyler
 * @since 2022/2/28
 */
@Service
@RequiredArgsConstructor
public class OrderUserCacheService {
    public static final String REDIS_ORDER_RETAILER_KEY_PREFIX = "order-user-";

    @Resource
    private RedisTemplate<String, OrderUser> orderUserRedisTemplate;

    private RedisScript<String> canalSync;
    private RedisScript<String> batchInsertOrderUser;
    private RedisScript<String> batchDeleteOrderUser;
    private SetOperations<String, OrderUser> setOperations;

    @PostConstruct
    void init() {
        setOperations = orderUserRedisTemplate.opsForSet();
        canalSync = RedisScript.of(new ClassPathResource("lua/canalSyncOrderUserScript.lua"), String.class);
        batchInsertOrderUser = RedisScript.of(new ClassPathResource("lua/batchInsertOrderUserScript.lua"), String.class);
        batchDeleteOrderUser = RedisScript.of(new ClassPathResource("lua/batchDeleteOrderUserScript.lua"), String.class);
    }

    public List<OrderUser> get(Long userId) {
        return setOperations.members(REDIS_ORDER_RETAILER_KEY_PREFIX + userId)
                .parallelStream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    //    返回未添加成功的元素
    public List<OrderUser> insertBatch(List<OrderUser> orderUserList) {
        String arrayString = orderUserRedisTemplate.execute(batchInsertOrderUser, Collections.singletonList(REDIS_ORDER_RETAILER_KEY_PREFIX), orderUserList.toArray());
        return arrayString.equals("{}") ? new ArrayList<>() : JSONArray.parseArray(arrayString, OrderUser.class);
    }

    //    返回不存在的元素
    public List<OrderUser> deleteBatch(List<OrderUser> orderUserList) {
        String arrayString = orderUserRedisTemplate.execute(batchDeleteOrderUser, Collections.singletonList(REDIS_ORDER_RETAILER_KEY_PREFIX), orderUserList.toArray());
        return arrayString.equals("{}") ? new ArrayList<>() : JSONArray.parseArray(arrayString, OrderUser.class);
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        orderUserRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_ORDER_RETAILER_KEY_PREFIX), redisCommandList.toArray());
    }
}
