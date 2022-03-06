package com.waibao.order;

import com.waibao.util.async.AsyncService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * OrderApplication
 *
 * @author alexpetertyler
 * @since 2022/2/28
 */
@EnableAsync
@EnableScheduling
@EnableDiscoveryClient
@SpringBootApplication
@Import({AsyncService.class})
@MapperScan("com.waibao.order.mapper")
public class OrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderApplication.class, args);
    }
}
