package com.waibao.seckill.service.cache;

import cn.hutool.core.lang.id.NanoId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.Collections;

/**
 * RandomStrCacheService
 *
 * @author alexpetertyler
 * @since 2022-02-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeckillPathCacheService {
    @Resource
    private RedisTemplate<String, Long> goodsRetailerRedisTemplate;

    private ValueOperations<String, Long> valueOperations;
    private RedisScript<Boolean> deleteSeckillPath;

    @PostConstruct
    public void init() {
        valueOperations = goodsRetailerRedisTemplate.opsForValue();
        deleteSeckillPath = RedisScript.of(new ClassPathResource("lua/deleteSeckillPath.lua"), Boolean.class);
    }

    public boolean delete(String randomStr, Long goodsId) {
        return Boolean.TRUE.equals(valueOperations.getOperations()
                .execute(deleteSeckillPath, Collections.singletonList(randomStr), goodsId));
    }

    public String set(Long goodsId) {
        String key = NanoId.randomNanoId(20);
        valueOperations.set(key, goodsId);
        return key;
    }

}
