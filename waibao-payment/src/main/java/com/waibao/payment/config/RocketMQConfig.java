package com.waibao.payment.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
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
    private final RocketMQProperties rocketMQProperties;

//    @Bean
//    @SneakyThrows
//    public DefaultMQProducer orderCancelMQProducer() {
//        DefaultMQProducer orderCancel = new DefaultMQProducer("orderCancel");
//        orderCancel.setNamesrvAddr(rocketMQProperties.getNameServer());
//        orderCancel.start();
//        return orderCancel;
//    }

    private DefaultMQPushConsumer getSingleThreadBatchConsumer() {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer();
        consumer.setNamesrvAddr(rocketMQProperties.getNameServer());
        consumer.setPullInterval(1000);
        consumer.setConsumeThreadMax(1);
        consumer.setConsumeThreadMin(1);
        consumer.setPullBatchSize(760);
        consumer.setMaxReconsumeTimes(3);
        consumer.setConsumeMessageBatchMaxSize(760);
        return consumer;
    }

}