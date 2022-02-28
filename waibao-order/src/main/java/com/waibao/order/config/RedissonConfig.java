package com.waibao.order.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RedissonConfig
 *
 * @author alexpetertyler
 * @since 2022-02-17
 */
@Configuration
public class RedissonConfig {
    @Value("${spring.redis.host}")
    private String url;
    @Value("${spring.redis.password}")
    private String password;

    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.setLockWatchdogTimeout(10000L);
        SingleServerConfig singleServerConfig = config.useSingleServer();
        singleServerConfig.setPassword(password);
        singleServerConfig.setAddress("redis://" + url + ":6379");
        singleServerConfig.setDatabase(1);
        return Redisson.create(config);
    }
}
