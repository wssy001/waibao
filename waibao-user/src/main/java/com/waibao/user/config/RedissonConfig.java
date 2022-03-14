package com.waibao.user.config;

import lombok.RequiredArgsConstructor;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RedissonConfig
 *
 * @author alexpetertyler
 * @since 2022-02-17
 */
@Configuration
@RequiredArgsConstructor
public class RedissonConfig {
    private final RedisProperties redisProperties;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.setLockWatchdogTimeout(10000L);
        SingleServerConfig singleServerConfig = config.useSingleServer();
        singleServerConfig.setPassword(redisProperties.getPassword());
        singleServerConfig.setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort());
        singleServerConfig.setDatabase(1);
        return Redisson.create(config);
    }
}
