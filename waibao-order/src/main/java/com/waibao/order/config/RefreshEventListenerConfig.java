package com.waibao.order.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cloud.endpoint.event.RefreshEvent;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * RefreshEventListenerConfig
 *
 * @author alexpetertyler
 * @since 2022-02-17
 */
@Configuration
@RequiredArgsConstructor
public class RefreshEventListenerConfig {
    private AtomicBoolean ready = new AtomicBoolean(false);

    @EventListener
    public void listen(ApplicationEvent event) {
        if (event instanceof ApplicationReadyEvent)
            this.ready.compareAndSet(false, true);

        if (event instanceof RefreshEvent && this.ready.get()){
            // 配置文件已更新

        }
    }
}
