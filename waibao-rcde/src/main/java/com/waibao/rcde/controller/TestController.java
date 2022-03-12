package com.waibao.rcde.controller;

import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * TestController
 *
 * @author alexpetertyler
 * @since 2022/3/8
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TestController {
    private final Executor dbThreadPoolExecutor;

    @GetMapping("/test")
    public void test() {
        Flux.interval(Duration.of(10, ChronoUnit.MILLIS))
                .log()
                .onBackpressureBuffer(2)
                .flatMap(this::doBlocking)
                .subscribe();
    }

    private Mono<Long> doBlocking(Long i) {
        return Mono.fromFuture(CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(1000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            long j = RandomUtil.randomLong(i + 1000);
            log.info("******ReactorTest.doBlocking：{}", j);
            return j;
        }, dbThreadPoolExecutor));
    }
}
