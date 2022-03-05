package com.waibao.order.service.cache;

import com.waibao.order.entity.LogOrderRetailer;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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
public class LogOrderRetailerCacheService {
    public static final String REDIS_LOG_ORDER_RETAILER_KEY_PREFIX = "log-order-retailer-";

    @Resource
    private RedisTemplate<String, String> logOrderRetailerRedisTemplate;

    private DefaultRedisScript<String> canalSync;
    private ListOperations<String, String> listOperations;
    private DefaultRedisScript<Boolean> checkRetailerOperation;
    private DefaultRedisScript<String> batchInsertOrderRetailer;

    @PostConstruct
    void init() {
        String checkOperationScript = "local key = KEYS[1]\n" +
                "local count = tonumber(redis.call('LREM', key, 0, ARGV[1] .. '-' .. ARGV[2]))\n" +
                "if count > 0 then\n" +
                "    redis.call('LPUSH', key, ARGV[1] .. '-' .. ARGV[2])\n" +
                "    return true\n" +
                "else\n" +
                "    return false\n" +
                "end";
        String batchInsertScript = "local key = KEYS[1]\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local logOrderRetailer = cjson.decode(value)\n" +
                "    key = '\"' .. string.gsub(key, '\"', '') .. logOrderRetailer['retailerId'] .. '\"'\n" +
                "    redis.call('LPUSH', key, logOrderRetailer['orderId'] .. '-' .. logOrderRetailer['operation'])\n" +
                "end";
        String canalSyncScript = "local key = KEYS[1]\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local redisCommand = cjson.decode(value)\n" +
                "    local logOrderRetailer = redisCommand['value']\n" +
                "    key = '\"' .. string.gsub(key, '\"', '') .. logOrderRetailer['retailerId'] .. '\"'\n" +
                "    local command = redisCommand['command']\n" +
                "    if command == 'INSERT' then\n" +
                "        redis.call('LPUSH', key, logOrderRetailer['orderId'] .. '-' .. logOrderRetailer['operation'])\n" +
                "    elseif command == 'UPDATE' then\n" +
                "        local oldLogOrderRetailer = redisCommand['oldValue']\n" +
                "        if oldLogOrderRetailer['operation'] ~= nil then\n" +
                "            redis.call('LPUSH', key, logOrderRetailer['orderId'] .. '-' .. logOrderRetailer['operation'])\n" +
                "            redis.call('LREM', key, 0, logOrderRetailer['orderId'] .. '-' .. oldLogOrderRetailer['operation'])\n" +
                "        end\n" +
                "    else\n" +
                "        redis.call('LREM', key, 0, logOrderRetailer['orderId'] .. '-' .. logOrderRetailer['operation'])\n" +
                "    end\n" +
                "end";
        canalSync = new DefaultRedisScript<>(canalSyncScript);
        listOperations = logOrderRetailerRedisTemplate.opsForList();
        batchInsertOrderRetailer = new DefaultRedisScript<>(batchInsertScript);
        checkRetailerOperation = new DefaultRedisScript<>(checkOperationScript, Boolean.class);
    }

    public boolean checkOperation(Long userId, String orderId, String operation) {
        return Boolean.TRUE.equals(logOrderRetailerRedisTemplate.execute(checkRetailerOperation, Collections.singletonList(REDIS_LOG_ORDER_RETAILER_KEY_PREFIX + userId), orderId, operation));
    }

    public void insertBatch(List<LogOrderRetailer> logOrderRetailers) {
        logOrderRetailerRedisTemplate.execute(batchInsertOrderRetailer, Collections.singletonList(REDIS_LOG_ORDER_RETAILER_KEY_PREFIX), logOrderRetailers.toArray());
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        logOrderRetailerRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_LOG_ORDER_RETAILER_KEY_PREFIX), redisCommandList.toArray());
    }
}
