package com.waibao.rcde;

import com.waibao.util.async.AsyncService;
import com.waibao.util.thread.DBThreadPoolExecutorConfig;
import com.waibao.util.thread.MQThreadPoolExecutorConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * RCDEApplication
 *
 * @author alexpetertyler
 * @since 2022/3/8
 */
@EnableAsync
@SpringBootApplication
@MapperScan("com.waibao.rcde.mapper")
@Import({AsyncService.class, MQThreadPoolExecutorConfig.class, DBThreadPoolExecutorConfig.class})
public class RCDEApplication {
    public static void main(String[] args) {
        SpringApplication.run(RCDEApplication.class, args);
    }
}
