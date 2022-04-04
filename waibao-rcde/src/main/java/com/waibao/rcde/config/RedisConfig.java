package com.waibao.rcde.config;

import com.waibao.util.enums.RedisDBEnum;
import lombok.RequiredArgsConstructor;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {
    private final RedisProperties redisProperties;

    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate() {
        return getReactiveRedisTemplate(RedisDBEnum.Default);
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> userReactiveRedisTemplate() {
        return getReactiveRedisTemplate(RedisDBEnum.User);
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> userExtraReactiveRedisTemplate() {
        return getReactiveRedisTemplate(RedisDBEnum.User);
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> depositReactiveRedisTemplate() {
        return getReactiveRedisTemplate(RedisDBEnum.Deposit);
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> ruleReactiveRedisTemplate() {
        return getReactiveRedisTemplate(RedisDBEnum.Rule);
    }

    @Bean
    public ReactiveRedisTemplate<String, Object> riskUserReactiveRedisTemplate() {
        return getReactiveRedisTemplate(RedisDBEnum.RiskUser);
    }

    private ReactiveRedisTemplate<String, Object> getReactiveRedisTemplate(RedisDBEnum storage) {
        return new ReactiveRedisTemplate<>(createLettuceConnectionFactory(storage), getObjectRedisSerializationContext());
    }

    private LettuceConnectionFactory createLettuceConnectionFactory(RedisDBEnum redisDBEnum) {
        // Redis配置
        RedisStandaloneConfiguration redisConfiguration = new RedisStandaloneConfiguration(redisProperties.getHost(), redisProperties.getPort());
        redisConfiguration.setDatabase(redisDBEnum.getIndex());
        redisConfiguration.setPassword(redisProperties.getPassword());

        // 连接池配置
        RedisProperties.Pool pool = redisProperties.getLettuce().getPool();
        GenericObjectPoolConfig<Object> genericObjectPoolConfig = new GenericObjectPoolConfig<>();
        genericObjectPoolConfig.setMaxIdle(pool.getMaxIdle());
        genericObjectPoolConfig.setMinIdle(pool.getMinIdle());
        genericObjectPoolConfig.setMaxTotal(pool.getMaxActive());
        genericObjectPoolConfig.setMaxWaitMillis(pool.getMaxWait().toMillis());

        // Redis客户端配置
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration
                .builder().commandTimeout(redisProperties.getTimeout());

        builder.shutdownTimeout(Duration.ofMillis(100));
        builder.poolConfig(genericObjectPoolConfig);

        // 根据配置和客户端配置创建连接
        LettuceClientConfiguration lettuceClientConfiguration = builder.build();
        LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(redisConfiguration,
                lettuceClientConfiguration);
        lettuceConnectionFactory.afterPropertiesSet();
        return lettuceConnectionFactory;
    }

    private RedisSerializationContext<String, Object> getObjectRedisSerializationContext() {
        return RedisSerializationContext
                .<String, Object>newSerializationContext(new GenericToStringSerializer<>(Object.class))
                .key(new GenericToStringSerializer<>(String.class))
                .value(new GenericToStringSerializer<>(Object.class))
                .hashKey(new GenericToStringSerializer<>(String.class))
                .hashValue(new GenericToStringSerializer<>(String.class))
                .build();
    }

}
