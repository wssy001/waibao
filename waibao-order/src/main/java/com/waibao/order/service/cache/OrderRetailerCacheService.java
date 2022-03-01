package com.waibao.order.service.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.waibao.order.entity.OrderRetailer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.Future;
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
    public static final String REDIS_ORDER_RETAILER_KEY = "order-retailer-";

    @Resource
    private RedisTemplate<String, OrderRetailer> orderRetailerRedisTemplate;

    private SetOperations<String, OrderRetailer> setOperations;
    private DefaultRedisScript<String> batchInsertOrderRetailer;
    private DefaultRedisScript<String> batchDeleteOrderRetailer;

    @PostConstruct
    void init() {
        String batchInsertScript = "local key = KEYS[1]\n" +
                "local orderRetailerList = {}\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local orderRetailer = cjson.decode(value)\n" +
                "    local count = tonumber(redis.call('SETNX', key .. orderRetailer[\"retailerId\"], orderRetailer[\"orderId\"]))\n" +
                "    if count == 0 then\n" +
                "        table.insert(orderRetailerList, orderRetailer)\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(orderRetailerList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(orderRetailerList)\n" +
                "end";
        String batchDeleteScript = "local key = KEYS[1]\n" +
                "local orderRetailerList = {}\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local orderRetailer = cjson.decode(value)\n" +
                "    local count = tonumber(redis.call('SREM', key .. orderRetailer[\"retailerId\"], orderRetailer[\"orderId\"]))\n" +
                "    if count == 0 then\n" +
                "        table.insert(orderRetailerList, orderRetailer)\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(orderRetailerList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(orderRetailerList)\n" +
                "end";
        setOperations = orderRetailerRedisTemplate.opsForSet();
        batchInsertOrderRetailer = new DefaultRedisScript<>(batchInsertScript, String.class);
        batchDeleteOrderRetailer = new DefaultRedisScript<>(batchDeleteScript, String.class);
    }

    public List<OrderRetailer> get(Long retailerId) {
        return setOperations.members(REDIS_ORDER_RETAILER_KEY + retailerId)
                .parallelStream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    //    返回未添加成功的元素
    public List<OrderRetailer> insertBatch(List<OrderRetailer> orderRetailerList) {
        String arrayString = orderRetailerRedisTemplate.execute(batchInsertOrderRetailer, Collections.singletonList(REDIS_ORDER_RETAILER_KEY), orderRetailerList.toArray());
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, OrderRetailer.class);
    }

    @Async
    public Future<List<OrderRetailer>> insertBatchAsync(List<OrderRetailer> orderRetailerList) {
        return new AsyncResult<>(insertBatch(orderRetailerList));
    }

    //    返回不存在的元素
    public List<OrderRetailer> deleteBatch(List<OrderRetailer> orderRetailerList) {
        String arrayString = orderRetailerRedisTemplate.execute(batchDeleteOrderRetailer, Collections.singletonList(REDIS_ORDER_RETAILER_KEY), orderRetailerList.toArray());
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, OrderRetailer.class);
    }

    @Async
    public Future<List<OrderRetailer>> deleteBatchAsync(List<OrderRetailer> orderRetailerList) {
        return new AsyncResult<>(deleteBatch(orderRetailerList));
    }
}
