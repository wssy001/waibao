package com.waibao.seckill.service.cache;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSONArray;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * PurchasedUserCacheService
 *
 * @author alexpetertyler
 * @since 2022-02-18
 */
@Service
@RequiredArgsConstructor
public class PurchasedUserCacheService {
    public static final String REDIS_PURCHASED_USER_KEY = "purchased-user-";

    @Resource
    private RedisTemplate<String, Integer> userRedisTemplate;

    private ValueOperations<String, Integer> valueOperations;
    private DefaultRedisScript<String> decrease;
    private DefaultRedisScript<Boolean> increaseCount;
    private DefaultRedisScript<Boolean> reachLimit;

    @PostConstruct
    public void init() {
        String increaseCountScript = "local key = KEYS[1] \n" +
                "local count = tonumber(ARGV[1]) \n" +
                "local limit = tonumber(ARGV[2]) \n" +
                "redis.call('INCRBY',key,count)\n" +
                "local currentCount = tonumber(redis.call('GET',key)) \n" +
                "if (currentCount > limit) then\n" +
                "redis.call('DECRBY',key,count)\n" +
                "return false\n" +
                "else\n" +
                "return true\n" +
                "end";
        String reachLimitScript = "local key = KEYS[1]\n" +
                "local count = tonumber(ARGV[1])\n" +
                "local currentCount = tonumber(redis.call('GET',key))\n" +
                "if (currentCount == nil) then return false\n" +
                "end\n" +
                "return(currentCount >= limit)";
        String decreaseScript = "local key = KEYS[1]\n" +
                "local orderVOList = {}\n" +
                "for index, value in ipairs(ARGV) do\n" +
                "    local orderVO = cjson.decode(value)\n" +
                "    key = '\"' .. string.gsub(key, '\"', '') .. orderVO['userId'] .. '\"'\n" +
                "    local count = tonumber(redis.call('DECR', key))\n" +
                "    if count < 0 then\n" +
                "        redis.call('SET', key, 0)\n" +
                "        table.insert(orderVOList, orderVO)\n" +
                "    end\n" +
                "end\n" +
                "if table.maxn(orderVOList) == 0 then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(orderVOList)\n" +
                "end";
        valueOperations = userRedisTemplate.opsForValue();
        increaseCount = new DefaultRedisScript<>(increaseCountScript, Boolean.class);
        reachLimit = new DefaultRedisScript<>(reachLimitScript, Boolean.class);
        decrease = new DefaultRedisScript<>(decreaseScript, String.class);
    }

    public Boolean increase(Long userId, int count, int limit) {
        return userRedisTemplate.execute(increaseCount, Collections.singletonList(REDIS_PURCHASED_USER_KEY + userId), count, limit);
    }

    public List<OrderVO> decreaseBatch(List<OrderVO> orderVOList) {
        String jsonArray = userRedisTemplate.execute(decrease, Collections.singletonList(REDIS_PURCHASED_USER_KEY), orderVOList.toArray());
        if (StrUtil.isBlank(jsonArray)) return new ArrayList<>();
        return JSONArray.parseArray(jsonArray, OrderVO.class);
    }

    public boolean reachLimit(Long userId, int limit) {
        Boolean result = userRedisTemplate.execute(reachLimit, Collections.singletonList(REDIS_PURCHASED_USER_KEY + userId), limit);
        return Boolean.TRUE.equals(result);
    }
}
