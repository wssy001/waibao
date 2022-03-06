package com.waibao.order.config;

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
import org.springframework.data.redis.core.RedisTemplate;

import java.time.Duration;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {
    private final RedisProperties redisProperties;

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
        LettuceConnectionFactory lettuceConnectionFactory = createLettuceConnectionFactory(RedisDBEnum.Default);
        return getStringObjectRedisTemplate(lettuceConnectionFactory);
    }

    @Bean
    public RedisTemplate<String, Object> orderRetailerRedisTemplate() {
        LettuceConnectionFactory lettuceConnectionFactory = createLettuceConnectionFactory(RedisDBEnum.OrderRetailer);
        return getStringObjectRedisTemplate(lettuceConnectionFactory);
    }

    @Bean
    public RedisTemplate<String, Object> orderUserRedisTemplate() {
        LettuceConnectionFactory lettuceConnectionFactory = createLettuceConnectionFactory(RedisDBEnum.OrderUser);
        return getStringObjectRedisTemplate(lettuceConnectionFactory);
    }

    @Bean
    public RedisTemplate<String, Object> orderGoodsRedisTemplate() {
        LettuceConnectionFactory lettuceConnectionFactory = createLettuceConnectionFactory(RedisDBEnum.OrderGoods);
        return getStringObjectRedisTemplate(lettuceConnectionFactory);
    }

    @Bean
    public RedisTemplate<String, Object> logOrderUserRedisTemplate() {
        LettuceConnectionFactory lettuceConnectionFactory = createLettuceConnectionFactory(RedisDBEnum.LogOrder);
        return getStringObjectRedisTemplate(lettuceConnectionFactory);
    }

    @Bean
    public RedisTemplate<String, Object> logOrderRetailerRedisTemplate() {
        LettuceConnectionFactory lettuceConnectionFactory = createLettuceConnectionFactory(RedisDBEnum.LogOrder);
        return getStringObjectRedisTemplate(lettuceConnectionFactory);
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

    private RedisTemplate<String, Object> getStringObjectRedisTemplate(LettuceConnectionFactory lettuceConnectionFactory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(lettuceConnectionFactory);
        redisTemplate.setHashKeySerializer(new FastJson2JsonRedisSerializer<>(String.class));
        redisTemplate.setHashValueSerializer(new FastJson2JsonRedisSerializer<>(Object.class));
        redisTemplate.setKeySerializer(new FastJson2JsonRedisSerializer<>(String.class));
        redisTemplate.setValueSerializer(new FastJson2JsonRedisSerializer<>(Object.class));
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
