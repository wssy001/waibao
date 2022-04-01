package com.waibao.user;

import com.waibao.util.async.AsyncService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * UserApplication
 *
 * @author alexpetertyler
 * @since 2022-02-15
 */
@EnableScheduling
@SpringBootApplication
@EnableDiscoveryClient
@Import({AsyncService.class})
@MapperScan("com.waibao.user.mapper")
public class UserApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
