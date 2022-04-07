package com.waibao.seckill.config;

import com.waibao.seckill.service.mq.*;
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
    private final RedisGoodsCanalConsumer redisGoodsCanalConsumer;
    private final StorageDecreaseConsumer storageDecreaseConsumer;
    private final StorageRollbackConsumer storageRollbackConsumer;
    private final RedisStorageRollbackConsumer redisStorageRollbackConsumer;

    @Bean
    @SneakyThrows
    public DefaultMQProducer orderCreateMQProducer() {
        DefaultMQProducer orderCreate = new DefaultMQProducer("orderCreate");
        orderCreate.setProducerGroup("order-producer");
        orderCreate.setNamesrvAddr(rocketMQProperties.getNameServer());
        orderCreate.start();
        return orderCreate;
    }

    @Bean
    @SneakyThrows
    public DefaultMQProducer orderUpdateMQProducer() {
        DefaultMQProducer orderUpdate = new DefaultMQProducer("orderUpdate");
        orderUpdate.setProducerGroup("order-producer");
        orderUpdate.setNamesrvAddr(rocketMQProperties.getNameServer());
        orderUpdate.start();
        return orderUpdate;
    }

    @Bean
    @SneakyThrows
    public DefaultMQProducer orderCancelMQProducer() {
        DefaultMQProducer orderCancel = new DefaultMQProducer("orderCancel");
        orderCancel.setProducerGroup("order-producer");
        orderCancel.setNamesrvAddr(rocketMQProperties.getNameServer());
        orderCancel.start();
        return orderCancel;
    }

    @Bean
    @SneakyThrows
    public DefaultMQProducer seckillCompensationMQProducer() {
        DefaultMQProducer seckillCancel = new DefaultMQProducer("seckillCompensation");
        seckillCancel.setNamesrvAddr(rocketMQProperties.getNameServer());
        seckillCancel.start();
        return seckillCancel;
    }

    @Bean
    @SneakyThrows
    public DefaultMQProducer orderCompensationMQProducer() {
        DefaultMQProducer orderCompensation = new DefaultMQProducer("orderCompensation");
        orderCompensation.setProducerGroup("order-producer");
        orderCompensation.setNamesrvAddr(rocketMQProperties.getNameServer());
        orderCompensation.start();
        return orderCompensation;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer redisStorageRollbackBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(redisStorageRollbackConsumer);
        consumer.setConsumerGroup("redisStorageRollback");
        consumer.subscribe("storage", "rollback||redisRollback");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer goodsCanalConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(redisGoodsCanalConsumer);
        consumer.setConsumerGroup("redisGoodsCanal");
        consumer.subscribe("waibao_v2_seckill_goods", "*");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer storageDecreaseBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(storageDecreaseConsumer);
        consumer.setConsumerGroup("storageDecrease");
        consumer.subscribe("storage", "decrease");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer storageRollbackBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(storageRollbackConsumer);
        consumer.setConsumerGroup("storageRollback");
        consumer.subscribe("storage", "rollback");
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
