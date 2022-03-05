package com.waibao.order.service.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.waibao.order.entity.OrderUser;
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

    private DefaultRedisScript<String> canalSync;
    private SetOperations<String, OrderUser> setOperations;
    private DefaultRedisScript<String> batchInsertOrderUser;
    private DefaultRedisScript<String> batchDeleteOrderUser;

    @PostConstruct
    void init() {
        String batchInsertScript = "local key = KEYS[1]\n" +
                "local orderUserList = {}\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local orderUser = cjson.decode(value)\n" +
                "    local count = tonumber(redis.call('SETNX', key .. orderUser['userId'], orderUser['orderId']))\n" +
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
                "    local count = tonumber(redis.call('SREM', key .. orderUser['userId'], orderUser['orderId']))\n" +
                "    if count == 0 then\n" +
                "        table.insert(orderUserList, orderUser)\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(orderUserList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(orderUserList)\n" +
                "end";
        String canalSyncScript = "local key = KEYS[1]\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local redisCommand = cjson.decode(value)\n" +
                "    local orderUser = redisCommand['value']\n" +
                "    key = '\"' .. string.gsub(key, '\"', '') .. orderUser['userId'] .. '\"'\n" +
                "    if redisCommand['command'] == 'SET' then\n" +
                "        orderUser['@type'] = 'com.waibao.order.entity.OrderUser'\n" +
                "        redis.call('SET', key, cjson.encode(orderUser))\n" +
                "    else\n" +
                "        redis.call('DEL', key)\n" +
                "    end\n" +
                "end";
        canalSync = new DefaultRedisScript<>(canalSyncScript);
        setOperations = orderUserRedisTemplate.opsForSet();
        batchInsertOrderUser = new DefaultRedisScript<>(batchInsertScript, String.class);
        batchDeleteOrderUser = new DefaultRedisScript<>(batchDeleteScript, String.class);
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
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, OrderUser.class);
    }

    //    返回不存在的元素
    public List<OrderUser> deleteBatch(List<OrderUser> orderUserList) {
        String arrayString = orderUserRedisTemplate.execute(batchDeleteOrderUser, Collections.singletonList(REDIS_ORDER_RETAILER_KEY_PREFIX), orderUserList.toArray());
        if (StrUtil.isBlank(arrayString)) return new ArrayList<>();
        return JSONArray.parseArray(arrayString, OrderUser.class);
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        orderUserRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_ORDER_RETAILER_KEY_PREFIX), redisCommandList.toArray());
    }
}
