package com.waibao.order.service.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.waibao.order.entity.OrderRetailer;
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
 * OrderRetailerCacheService
 *
 * @author alexpetertyler
 * @since 2022/2/28
 */
@Service
@RequiredArgsConstructor
public class OrderRetailerCacheService {
    public static final String REDIS_ORDER_RETAILER_KEY_PREFIX = "order-retailer-";

    @Resource
    private RedisTemplate<String, OrderRetailer> orderRetailerRedisTemplate;

    private RedisScript<String> canalSync;
    private SetOperations<String, OrderRetailer> setOperations;
    private RedisScript<String> batchInsertOrderRetailer;
    private RedisScript<String> batchDeleteOrderRetailer;

    @PostConstruct
    void init() {
        setOperations = orderRetailerRedisTemplate.opsForSet();
        canalSync = RedisScript.of(new ClassPathResource("lua/canalSyncOrderRetailerScript.lua"), String.class);
        batchInsertOrderRetailer = RedisScript.of(new ClassPathResource("lua/batchInsertOrderRetailerScript.lua"), String.class);
        batchDeleteOrderRetailer = RedisScript.of(new ClassPathResource("lua/batchDeleteOrderRetailerScript.lua"), String.class);
    }

    public List<OrderRetailer> get(Long retailerId) {
        return setOperations.members(REDIS_ORDER_RETAILER_KEY_PREFIX + retailerId)
                .parallelStream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    //    返回未添加成功的元素
    public List<OrderRetailer> insertBatch(List<OrderRetailer> orderRetailerList) {
        String arrayString = orderRetailerRedisTemplate.execute(batchInsertOrderRetailer, Collections.singletonList(REDIS_ORDER_RETAILER_KEY_PREFIX), orderRetailerList.toArray());
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, OrderRetailer.class);
    }

    //    返回不存在的元素
    public List<OrderRetailer> deleteBatch(List<OrderRetailer> orderRetailerList) {
        String arrayString = orderRetailerRedisTemplate.execute(batchDeleteOrderRetailer, Collections.singletonList(REDIS_ORDER_RETAILER_KEY_PREFIX), orderRetailerList.toArray());
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, OrderRetailer.class);
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        orderRetailerRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_ORDER_RETAILER_KEY_PREFIX), redisCommandList.toArray());
    }
}
