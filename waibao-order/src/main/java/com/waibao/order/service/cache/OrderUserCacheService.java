package com.waibao.order.service.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.waibao.order.entity.OrderUser;
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
 * OrderUserCacheService
 *
 * @author alexpetertyler
 * @since 2022/2/28
 */
@Service
@RequiredArgsConstructor
public class OrderUserCacheService {
    public static final String REDIS_ORDER_RETAILER_KEY = "order-user-";

    @Resource
    private RedisTemplate<String, OrderUser> orderUserRedisTemplate;

    private SetOperations<String, OrderUser> setOperations;
    private DefaultRedisScript<String> batchInsertOrderUser;
    private DefaultRedisScript<String> batchDeleteOrderUser;

    @PostConstruct
    void init() {
        String batchInsertScript = "local key = KEYS[1]\n" +
                "local orderUserList = {}\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local orderUser = cjson.decode(value)\n" +
                "    local count = tonumber(redis.call('SETNX', key .. orderUser[\"userId\"], orderUser[\"orderId\"]))\n" +
                "    if count == 0 then\n" +
                "        table.insert(orderUserList, orderUser)\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(orderUserList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(orderUserList)\n" +
                "end";
        String batchDeleteScript = "local key = KEYS[1]\n" +
                "local orderUserList = {}\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local orderUser = cjson.decode(value)\n" +
                "    local count = tonumber(redis.call('SREM', key .. orderUser[\"userId\"], orderUser[\"orderId\"]))\n" +
                "    if count == 0 then\n" +
                "        table.insert(orderUserList, orderUser)\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(orderUserList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(orderUserList)\n" +
                "end";
        setOperations = orderUserRedisTemplate.opsForSet();
        batchInsertOrderUser = new DefaultRedisScript<>(batchInsertScript, String.class);
        batchDeleteOrderUser = new DefaultRedisScript<>(batchDeleteScript, String.class);
    }

    public List<OrderUser> get(Long userId) {
        return setOperations.members(REDIS_ORDER_RETAILER_KEY + userId)
                .parallelStream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    //    返回未添加成功的元素
    public List<OrderUser> insertBatch(List<OrderUser> orderUserList) {
        String arrayString = orderUserRedisTemplate.execute(batchInsertOrderUser, Collections.singletonList(REDIS_ORDER_RETAILER_KEY), orderUserList.toArray());
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, OrderUser.class);
    }

    @Async
    public Future<List<OrderUser>> insertBatchAsync(List<OrderUser> orderUserList) {
        return new AsyncResult<>(insertBatch(orderUserList));
    }

    //    返回不存在的元素
    public List<OrderUser> deleteBatch(List<OrderUser> orderUserList) {
        String arrayString = orderUserRedisTemplate.execute(batchDeleteOrderUser, Collections.singletonList(REDIS_ORDER_RETAILER_KEY), orderUserList.toArray());
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, OrderUser.class);
    }

    @Async
    public Future<List<OrderUser>> deleteBatchAsync(List<OrderUser> orderUserList) {
        return new AsyncResult<>(deleteBatch(orderUserList));
    }
}
