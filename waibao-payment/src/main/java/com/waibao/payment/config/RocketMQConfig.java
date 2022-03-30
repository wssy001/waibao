package com.waibao.payment.config;

import com.waibao.payment.service.mq.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
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
    private final RedisPaymentCanalConsumer redisPaymentCanalConsumer;
    private final PaymentTransactionListener paymentTransactionListener;


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
        TransactionMQProducer transactionMQProducer = new TransactionMQProducer("paymentRequestPay");
        transactionMQProducer.setNamesrvAddr(rocketMQProperties.getNameServer());
        transactionMQProducer.start();
        return transactionMQProducer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQProducer paymentPayMQProducer() {
        TransactionMQProducer transactionMQProducer = new TransactionMQProducer("paymentPayTransaction");
        transactionMQProducer.setNamesrvAddr(rocketMQProperties.getNameServer());
        transactionMQProducer.setTransactionListener(paymentTransactionListener);
        transactionMQProducer.start();
        return transactionMQProducer;
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
    public DefaultMQPushConsumer paymentCanalConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(redisPaymentCanalConsumer);
        consumer.setConsumerGroup("paymentCanal");
        consumer.subscribe("waibao_payment_payment_0", "*");
        consumer.subscribe("waibao_payment_payment_1", "*");
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