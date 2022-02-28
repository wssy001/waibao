package com.waibao.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * OrderApplication
 *
 * @author alexpetertyler
 * @since 2022/2/28
 */
@Async
@EnableScheduling
@EnableDiscoveryClient
@SpringBootApplication
@MapperScan("com.waibao.order.mapper")
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
