package com.waibao.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SeckillApplication
 *
 * @author alexpetertyler
 * @since 2022-02-17
 */
@Async
@EnableScheduling
@EnableDiscoveryClient
@SpringBootApplication
@MapperScan("com.waibao.seckill.mapper")
public class SeckillApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}
