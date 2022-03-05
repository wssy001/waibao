package com.waibao.payment.config;

import com.waibao.payment.service.mq.*;
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
    private final PaymentCancelConsumer paymentCancelConsumer;
    private final PaymentCreateConsumer paymentCreateConsumer;
    private final PaymentDeleteConsumer paymentDeleteConsumer;
    private final PaymentUpdateConsumer paymentUpdateConsumer;
    private final UserCreditCancelConsumer userCreditCancelConsumer;
    private final UserCreditCreateConsumer userCreditCreateConsumer;
    private final UserCreditDeleteConsumer userCreditDeleteConsumer;
    private final UserCreditUpdateConsumer userCreditUpdateConsumer;
    private final RedisPaymentCanalConsumer redisPaymentCanalConsumer;
    private final RedisUserCreditCanalConsumer redisUserCreditCanalConsumer;

    @Bean
    @SneakyThrows
    public DefaultMQProducer paymentCreateMQProducer() {
        DefaultMQProducer orderCancel = new DefaultMQProducer("paymentCreate");
        orderCancel.setNamesrvAddr(rocketMQProperties.getNameServer());
        orderCancel.start();
        return orderCancel;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer paymentCancelDBBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(paymentCancelConsumer);
        consumer.setConsumerGroup("paymentCancel");
        consumer.subscribe("payment", "cancel");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer paymentCreateDBBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(paymentCreateConsumer);
        consumer.setConsumerGroup("paymentCreate");
        consumer.subscribe("order", "create");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer paymentDeleteDBBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(paymentDeleteConsumer);
        consumer.setConsumerGroup("paymentDelete");
        consumer.subscribe("payment", "delete");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer paymentUpdateDBBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(paymentUpdateConsumer);
        consumer.setConsumerGroup("paymentUpdate");
        consumer.subscribe("payment", "update");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer userCreditCancelRedisBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(userCreditCancelConsumer);
        consumer.setConsumerGroup("userCreditCancel");
        consumer.subscribe("userCredit", "cancel");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer userCreditCreateRedisBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(userCreditCreateConsumer);
        consumer.setConsumerGroup("userCreditCreate");
        consumer.subscribe("userCredit", "create");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer userCreditDeleteRedisBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(userCreditDeleteConsumer);
        consumer.setConsumerGroup("userCreditDelete");
        consumer.subscribe("userCredit", "delete");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer userCreditUpdateRedisBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(userCreditUpdateConsumer);
        consumer.setConsumerGroup("userCreditUpdate");
        consumer.subscribe("userCredit", "update");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer paymentCanalConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(redisPaymentCanalConsumer);
        consumer.setConsumerGroup("paymentCanal");
        consumer.subscribe("waibao_payment_payment_0", "*");
        consumer.subscribe("waibao_payment_payment_1", "*");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer userCreditCanalConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(redisUserCreditCanalConsumer);
        consumer.setConsumerGroup("userCreditCanal");
        consumer.subscribe("waibao_credit_user_user_credit_0", "*");
        consumer.subscribe("waibao_credit_user_user_credit_1", "*");
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