package com.waibao.order.config;

import com.waibao.order.service.mq.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
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
    private final RocketMQProperties rocketMQProperties;
    private final OrderCreateConsumer orderCreateConsumer;
    private final OrderUpdateConsumer orderUpdateConsumer;
    private final OrderCancelConsumer orderCancelConsumer;
    private final OrderDeleteConsumer orderDeleteConsumer;
    private final RedisOrderUserCanalConsumer redisOrderUserCanalConsumer;
    private final RedisOrderGoodsCanalConsumer redisOrderGoodsCanalConsumer;
    private final RedisLogOrderGoodsCanalConsumer redisLogOrderUserCanalConsumer;
    private final RedisOrderRetailerCanalConsumer redisOrderRetailerCanalConsumer;

    @Bean
    @SneakyThrows
    public DefaultMQProducer orderCancelMQProducer() {
        DefaultMQProducer orderCancel = new DefaultMQProducer("orderCancel");
        orderCancel.setNamesrvAddr(rocketMQProperties.getNameServer());
        orderCancel.start();
        return orderCancel;
    }

    @Bean
    @SneakyThrows
    public DefaultMQProducer orderDeleteMQProducer() {
        DefaultMQProducer orderCancel = new DefaultMQProducer("orderDelete");
        orderCancel.setNamesrvAddr(rocketMQProperties.getNameServer());
        orderCancel.start();
        return orderCancel;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer orderCreateDBBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(orderCreateConsumer);
        consumer.setConsumerGroup("orderCreate");
        consumer.subscribe("order", "create");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer orderUpdateDBBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(orderUpdateConsumer);
        consumer.setConsumerGroup("orderUpdate");
        consumer.subscribe("order", "update");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer orderCancelDBBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(orderCancelConsumer);
        consumer.setConsumerGroup("orderCancel");
        consumer.subscribe("order", "cancel");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer orderDeleteDBBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(orderDeleteConsumer);
        consumer.setConsumerGroup("orderDelete");
        consumer.subscribe("order", "delete");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer orderUserCanalConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(redisOrderUserCanalConsumer);
        consumer.setConsumerGroup("orderUserCanal");
        consumer.subscribe("waibao_order_user_order_user_0", "*");
        consumer.subscribe("waibao_order_user_order_user_1", "*");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer orderGoodsCanalConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(redisOrderGoodsCanalConsumer);
        consumer.setConsumerGroup("orderGoodsCanal");
        consumer.subscribe("waibao_order_user_order_user_0", "*");
        consumer.subscribe("waibao_order_user_order_user_1", "*");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer logOrderUserCanalConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(redisLogOrderUserCanalConsumer);
        consumer.setConsumerGroup("logOrderUserCanal");
        consumer.subscribe("waibao_order_user_order_user_0", "*");
        consumer.subscribe("waibao_order_user_order_user_1", "*");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer orderRetailerCanalConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(redisOrderRetailerCanalConsumer);
        consumer.setConsumerGroup("orderRetailerCanal");
        consumer.subscribe("waibao_order_retailer_order_retailer_0", "*");
        consumer.subscribe("waibao_order_retailer_order_retailer_1", "*");
        consumer.start();
        return consumer;
    }

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