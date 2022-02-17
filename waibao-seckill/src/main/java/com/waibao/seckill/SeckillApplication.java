package com.waibao.seckill;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * SeckillApplication
 *
 * @author alexpetertyler
 * @since 2022-02-17
 */
@EnableFeignClients
@EnableDiscoveryClient
@SpringBootApplication
public class SeckillApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}
