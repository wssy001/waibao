package com.waibao.seckill;

import com.waibao.util.async.AsyncService;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SeckillApplication
 *
 * @author alexpetertyler
 * @since 2022-02-17
 */
@Slf4j
@EnableAsync
@EnableScheduling
@EnableDiscoveryClient
@SpringBootApplication
@Import({AsyncService.class})
@MapperScan("com.waibao.seckill.mapper")
public class SeckillApplication {
    public static void main(String[] args) {
        SpringApplication.run(SeckillApplication.class, args);
    }
}
