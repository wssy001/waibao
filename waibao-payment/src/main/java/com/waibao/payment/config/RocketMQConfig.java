package com.waibao.payment.config;

import com.waibao.payment.service.mq.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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
@Configuration
@RequiredArgsConstructor
public class RocketMQConfig {
    private final RocketMQProperties rocketMQProperties;
    private final PaymentTestConsumer paymentTestConsumer;
    private final PaymentCancelConsumer paymentCancelConsumer;
    private final PaymentCreateConsumer paymentCreateConsumer;
    private final PaymentDeleteConsumer paymentDeleteConsumer;
    private final PaymentUpdateConsumer paymentUpdateConsumer;
    private final PaymentRequestPayConsumer paymentRequestPayConsumer;
    private final RedisPaymentCanalConsumer redisPaymentCanalConsumer;
    private final LogUserCreditCanalConsumer logUserCreditCanalConsumer;
    private final RedisLogPaymentCanalConsumer redisLogPaymentCanalConsumer;

    @Bean
    @SneakyThrows
    public DefaultMQProducer paymentCancelMQProducer() {
        DefaultMQProducer paymentCancelMQProducer = new DefaultMQProducer("paymentCancel");
        paymentCancelMQProducer.setNamesrvAddr(rocketMQProperties.getNameServer());
        paymentCancelMQProducer.start();
        return paymentCancelMQProducer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQProducer paymentRequestPayMQProducer() {
        DefaultMQProducer paymentRequestPayMQProducer = new DefaultMQProducer("paymentRequestPay");
        paymentRequestPayMQProducer.setNamesrvAddr(rocketMQProperties.getNameServer());
        paymentRequestPayMQProducer.start();
        return paymentRequestPayMQProducer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQProducer paymentUpdateMQProducer() {
        DefaultMQProducer paymentCancelMQProducer = new DefaultMQProducer("paymentUpdate");
        paymentCancelMQProducer.setNamesrvAddr(rocketMQProperties.getNameServer());
        paymentCancelMQProducer.start();
        return paymentCancelMQProducer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQProducer paymentCompensationMQProducer() {
        DefaultMQProducer paymentCancel = new DefaultMQProducer("paymentCompensation");
        paymentCancel.setNamesrvAddr(rocketMQProperties.getNameServer());
        paymentCancel.start();
        return paymentCancel;
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
    public DefaultMQPushConsumer paymentTestBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(paymentTestConsumer);
        consumer.setConsumerGroup("paymentTest");
        consumer.subscribe("order", "test");
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
    public DefaultMQPushConsumer paymentRequestPayBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(paymentRequestPayConsumer);
        consumer.setConsumerGroup("paymentRequestPay");
        consumer.subscribe("payment", "requestPay");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer paymentCanalConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(redisPaymentCanalConsumer);
        consumer.setConsumerGroup("paymentCanal");
        consumer.subscribe("waibao_v3_payment", "*");
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer logUserCreditCanalBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(logUserCreditCanalConsumer);
        consumer.setConsumerGroup("logUserCreditCanal");
        consumer.subscribe("waibao_v3_log_user_credit", "*");
        consumer.setPullInterval(5000);
        consumer.start();
        return consumer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer logPaymentCanalBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(redisLogPaymentCanalConsumer);
        consumer.setConsumerGroup("logPaymentCanal");
        consumer.subscribe("waibao_v3_log_payment", "*");
        consumer.start();
        return consumer;
    }

    private DefaultMQPushConsumer getSingleThreadBatchConsumer() {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer();
        consumer.setNamesrvAddr(rocketMQProperties.getNameServer());
        consumer.setConsumeThreadMax(1);
        consumer.setConsumeThreadMin(1);
        consumer.setPullBatchSize(100);
        consumer.setConsumeTimeout(1);
        consumer.setMaxReconsumeTimes(3);
        consumer.setConsumeMessageBatchMaxSize(100);
        return consumer;
    }

}