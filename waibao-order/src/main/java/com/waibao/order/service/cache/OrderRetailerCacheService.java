package com.waibao.order.service.cache;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.waibao.order.entity.OrderRetailer;
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
    private RedisTemplate<String, String> orderRetailerRedisTemplate;

    private RedisScript<String> canalSync;
    private RedisScript<String> getOrderRetailer;
    private RedisScript<String> batchGetOrderRetailer;
    private RedisScript<String> batchGetOrderRetailerByRetailerId;

    @PostConstruct
    void init() {
        getOrderRetailer = RedisScript.of(new ClassPathResource("lua/getOrderRetailerScript.lua"), String.class);
        canalSync = RedisScript.of(new ClassPathResource("lua/canalSyncOrderRetailerScript.lua"), String.class);
        batchGetOrderRetailer = RedisScript.of(new ClassPathResource("lua/batchGetOrderRetailerScript.lua"), String.class);
        batchGetOrderRetailerByRetailerId = RedisScript.of(new ClassPathResource("lua/batchGetOrderRetailerByRetailerIdScript.lua"), String.class);
    }

    public OrderRetailer get(Long retailerId, String orderId) {
        String execute = orderRetailerRedisTemplate.execute(getOrderRetailer, Collections.singletonList(REDIS_ORDER_RETAILER_KEY_PREFIX), retailerId + "", orderId);
        return JSON.parseObject(execute, OrderRetailer.class);
    }

    public List<OrderRetailer> getBatch(List<OrderVO> orderVOList) {
        String execute = orderRetailerRedisTemplate.execute(batchGetOrderRetailer, Collections.singletonList(REDIS_ORDER_RETAILER_KEY_PREFIX), JSONArray.toJSONString(orderVOList));
        if ("{}".equals(execute)) return new ArrayList<>();
        return JSONArray.parseArray(execute, OrderRetailer.class);
    }

//    public List<OrderRetailer> getBatch(Long retailerId) {
//        String execute = orderRetailerRedisTemplate.execute(batchGetOrderRetailerByRetailerId, Collections.singletonList(REDIS_ORDER_RETAILER_KEY_PREFIX), retailerId + "");
//        if ("{}".equals(execute)) return new ArrayList<>();
//        return JSONArray.parseArray(execute, OrderRetailer.class);
//    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        orderRetailerRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_ORDER_RETAILER_KEY_PREFIX), redisCommandList.toArray());
    }
}
