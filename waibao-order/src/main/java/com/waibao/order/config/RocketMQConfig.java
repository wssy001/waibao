package com.waibao.order.config;

import com.waibao.order.service.mq.OrderCancelConsumer;
import com.waibao.order.service.mq.OrderCreateConsumer;
import com.waibao.order.service.mq.OrderDeleteConsumer;
import com.waibao.order.service.mq.OrderUpdateConsumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQConfig
 *
 * @author alexpetertyler
 * @since 2022-02-17
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RocketMQConfig {
    private final OrderCreateConsumer orderCreateConsumer;
    private final OrderUpdateConsumer orderUpdateConsumer;
    private final OrderCancelConsumer orderCancelConsumer;
    private final OrderDeleteConsumer orderDeleteConsumer;
    private final RocketMQProperties rocketMQProperties;

    @Bean
    public DefaultMQPushConsumer orderCreateDBBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(orderCreateConsumer);
        consumer.setConsumerGroup("orderCreate");
        try {
            consumer.subscribe("order", "create");
            consumer.start();
        } catch (Exception e) {
            log.error("******RocketMQConfig.orderCreateDBBatchConsumer：{}", e.getMessage());
        }
        return consumer;
    }

    @Bean
    public DefaultMQPushConsumer orderUpdateDBBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(orderUpdateConsumer);
        consumer.setConsumerGroup("orderUpdate");
        try {
            consumer.subscribe("order", "update");
            consumer.start();
        } catch (Exception e) {
            log.error("******RocketMQConfig.orderUpdateDBBatchConsumer：{}", e.getMessage());
        }
        return consumer;
    }

    @Bean
    public DefaultMQPushConsumer orderCancelDBBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(orderCancelConsumer);
        consumer.setConsumerGroup("orderCancel");
        try {
            consumer.subscribe("order", "cancel");
            consumer.start();
        } catch (Exception e) {
            log.error("******RocketMQConfig.orderCancelDBBatchConsumer：{}", e.getMessage());
        }
        return consumer;
    }

    @Bean
    public DefaultMQPushConsumer orderDeleteDBBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(orderDeleteConsumer);
        consumer.setConsumerGroup("orderDelete");
        try {
            consumer.subscribe("order", "delete");
            consumer.start();
        } catch (Exception e) {
            log.error("******RocketMQConfig.orderDeleteDBBatchConsumer：{}", e.getMessage());
        }
        return consumer;
    }

    private DefaultMQPushConsumer getSingleThreadBatchConsumer() {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer();
        consumer.setNamesrvAddr(rocketMQProperties.getNameServer());
        consumer.setPullInterval(1000);
        consumer.setConsumeThreadMax(1);
        consumer.setConsumeThreadMin(1);
        consumer.setPullBatchSize(760);
        consumer.setConsumeMessageBatchMaxSize(760);
        return consumer;
    }


}
