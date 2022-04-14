package com.waibao.payment.service.cache;

import com.alibaba.fastjson.JSONArray;
import com.waibao.util.base.RedisCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;

/**
 * LogUserCreditCacheService
 *
 * @author alexpetertyler
 * @since 2022/4/14
 */
@Service
@RequiredArgsConstructor
public class LogUserCreditCacheService {
    private final String REDIS_LOG_USER_CREDIT_KEY = "log-user-credit";

    @Resource
    private RedisTemplate<String, String> logUserCreditRedisTemplate;

    private RedisScript<String> canalSync;

    @PostConstruct
    public void init() {
        canalSync = RedisScript.of(new ClassPathResource("lua/canalSyncLogUserCreditScript.lua"), String.class);
    }

    public void canalSync(List<RedisCommand> redisCommandList) {
        logUserCreditRedisTemplate.execute(canalSync, Collections.singletonList(REDIS_LOG_USER_CREDIT_KEY), JSONArray.toJSONString(redisCommandList));
    }
}
