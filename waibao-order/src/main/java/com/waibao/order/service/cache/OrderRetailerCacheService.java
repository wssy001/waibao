package com.waibao.order.service.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.waibao.order.entity.OrderRetailer;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
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

    private DefaultRedisScript<String> canalSync;
    private SetOperations<String, OrderRetailer> setOperations;
    private DefaultRedisScript<String> batchInsertOrderRetailer;
    private DefaultRedisScript<String> batchDeleteOrderRetailer;

    @PostConstruct
    void init() {
        String batchInsertScript = "local key = KEYS[1]\n" +
                "local orderRetailerList = {}\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local orderRetailer = cjson.decode(value)\n" +
                "    local count = tonumber(redis.call('SETNX', key .. orderRetailer['retailerId'], orderRetailer['orderId']))\n" +
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
                "    local count = tonumber(redis.call('SREM', key .. orderRetailer['retailerId'], orderRetailer['orderId']))\n" +
                "    if count == 0 then\n" +
                "        table.insert(orderRetailerList, orderRetailer)\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(orderRetailerList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(orderRetailerList)\n" +
                "end";
        String canalSyncScript = "local key = KEYS[1]\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local redisCommand = cjson.decode(value)\n" +
                "    local orderRetailer = redisCommand['value']\n" +
                "    key = '\\\"' .. string.gsub(key, '\\\"', '') .. orderRetailer['retailerId'] .. '\\\"'\n" +
                "    if redisCommand['command'] == 'SET' then\n" +
                "        orderRetailer['@type'] = 'com.waibao.order.entity.OrderRetailer'\n" +
                "        redis.call('SET', key, cjson.encode(orderRetailer))\n" +
                "    else\n" +
                "        redis.call('DEL', key)\n" +
                "    end\n" +
                "end";
        canalSync = new DefaultRedisScript<>(canalSyncScript);
        setOperations = orderRetailerRedisTemplate.opsForSet();
        batchInsertOrderRetailer = new DefaultRedisScript<>(batchInsertScript, String.class);
        batchDeleteOrderRetailer = new DefaultRedisScript<>(batchDeleteScript, String.class);
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
