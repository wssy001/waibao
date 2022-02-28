package com.waibao.seckill.service.captcha;

import com.anji.captcha.service.CaptchaCacheService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * CaptchaCacheServiceRedisImpl
 *
 * @author alexpetertyler
 * @since 2022/2/20
 */
@Service
public class CaptchaCacheServiceRedisImpl implements CaptchaCacheService {
    @Resource
    private RedisTemplate<String, String> CaptchaRedisTemplate;

    private ValueOperations<String, String> valueOperations;

    @PostConstruct
    void init() {
        valueOperations = CaptchaRedisTemplate.opsForValue();
    }

    @Override
    public String type() {
        return "redis";
    }

    @Override
    public void set(String key, String value, long expiresInSeconds) {
        valueOperations.set(key, value, expiresInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public boolean exists(String key) {
        return Boolean.TRUE.equals(valueOperations.getOperations().hasKey(key));
    }

    @Override
    public void delete(String key) {
        valueOperations.getOperations().delete(key);
    }

    @Override
    public String get(String key) {
        return valueOperations.get(key);
    }

    @Override
    public Long increment(String key, long val) {
        return valueOperations.increment(key, val);
    }
}
