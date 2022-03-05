package com.waibao.order.service.cache;

import com.waibao.order.entity.LogOrderUser;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
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
public class LogOrderUserCacheService {
    public static final String REDIS_LOG_ORDER_RETAILER_KEY_PREFIX = "log-order-user-";

    @Resource
    private RedisTemplate<String, String> logOrderUserRedisTemplate;

    private DefaultRedisScript<String> canalSync;
    private DefaultRedisScript<Boolean> checkUserOperation;
    private DefaultRedisScript<String> batchInsertOrderUser;

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
                "    local logOrderUser = cjson.decode(value)\n" +
                "    key = '\"' .. string.gsub(key, '\"', '') .. logOrderUser['userId'] .. '\"'\n" +
                "    redis.call('LPUSH', key, logOrderUser['orderId'] .. '-' .. logOrderUser['operation'])\n" +
                "end";
        String canalSyncScript = "local key = KEYS[1]\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local redisCommand = cjson.decode(value)\n" +
                "    local logOrderUser = redisCommand['value']\n" +
                "    key = '\"' .. string.gsub(key, '\"', '') .. logOrderUser['userId'] .. '\"'\n" +
                "    local command = redisCommand['command']\n" +
                "    if command == 'INSERT' then\n" +
                "        redis.call('LPUSH', key, logOrderUser['orderId'] .. '-' .. logOrderUser['operation'])\n" +
                "    elseif command == 'UPDATE' then\n" +
                "        local oldLogOrderUser = redisCommand['oldValue']\n" +
                "        if oldLogOrderUser['operation'] ~= nil then\n" +
                "            redis.call('LPUSH', key, logOrderUser['orderId'] .. '-' .. logOrderUser['operation'])\n" +
                "            redis.call('LREM', key, 0, logOrderUser['orderId'] .. '-' .. oldLogOrderUser['operation'])\n" +
                "        end\n" +
                "    else\n" +
                "        redis.call('LREM', key, 0, logOrderUser['orderId'] .. '-' .. logOrderUser['operation'])\n" +
                "    end\n" +
                "end";
        canalSync = new DefaultRedisScript<>(canalSyncScript);
        batchInsertOrderUser = new DefaultRedisScript<>(batchInsertScript);
        checkUserOperation = new DefaultRedisScript<>(checkOperationScript, Boolean.class);
    }

    public boolean checkOperation(Long userId, String orderId, String operation) {
        return Boolean.TRUE.equals(logOrderUserRedisTemplate.execute(checkUserOperation, Collections.singletonList(REDIS_LOG_ORDER_RETAILER_KEY_PREFIX + userId), orderId, operation));
    }

    public void insertBatch(List<LogOrderUser> logOrderUserList) {
        logOrderUserRedisTemplate.execute(batchInsertOrderUser, Collections.singletonList(REDIS_LOG_ORDER_RETAILER_KEY_PREFIX), logOrderUserList.toArray());
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        logOrderUserRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_LOG_ORDER_RETAILER_KEY_PREFIX), redisCommandList.toArray());
    }
}
