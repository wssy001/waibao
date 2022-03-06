package com.waibao.order.service.cache;

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
public class LogOrderGoodsCacheService {
    public static final String REDIS_LOG_ORDER_RETAILER_KEY_PREFIX = "log-order-goods-";

    @Resource
    private RedisTemplate<String, String> logOrderUserRedisTemplate;

    private DefaultRedisScript<String> canalSync;
    private DefaultRedisScript<Boolean> checkUserOperation;

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
        String canalSyncScript = "local key = KEYS[1]\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local redisCommand = cjson.decode(value)\n" +
                "    local logOrderUser = redisCommand['value']\n" +
                "    key = '\"' .. string.gsub(key, '\"', '') .. logOrderUser['goodsId'] .. '\"'\n" +
                "    local command = redisCommand['command']\n" +
                "    if command == 'INSERT' then\n" +
                "        redis.call('LPUSH', key, logOrderUser['orderId'] .. '-' .. logOrderUser['tags'])\n" +
                "    elseif command == 'UPDATE' then\n" +
                "        local oldLogOrderUser = redisCommand['oldValue']\n" +
                "        if oldLogOrderUser['operation'] ~= nil then\n" +
                "            redis.call('LPUSH', key, logOrderUser['orderId'] .. '-' .. logOrderUser['tags'])\n" +
                "            redis.call('LREM', key, 0, logOrderUser['orderId'] .. '-' .. oldLogOrderUser['tags'])\n" +
                "        end\n" +
                "    else\n" +
                "        redis.call('LREM', key, 0, logOrderUser['orderId'] .. '-' .. logOrderUser['tags'])\n" +
                "    end\n" +
                "end";
        canalSync = new DefaultRedisScript<>(canalSyncScript);
        checkUserOperation = new DefaultRedisScript<>(checkOperationScript, Boolean.class);
    }

    public boolean hasConsumedTags(Long goodsId, String orderId, String operation) {
        return Boolean.TRUE.equals(logOrderUserRedisTemplate.execute(checkUserOperation, Collections.singletonList(REDIS_LOG_ORDER_RETAILER_KEY_PREFIX + goodsId), orderId, operation));
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        logOrderUserRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_LOG_ORDER_RETAILER_KEY_PREFIX), redisCommandList.toArray());
    }
}
