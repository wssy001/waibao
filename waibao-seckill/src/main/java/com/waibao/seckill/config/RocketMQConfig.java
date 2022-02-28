package com.waibao.seckill.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${rocketmq.name-server}")
    private String nameServer;

//    @Bean
//    public DefaultMQPushConsumer imageMessageDBBatchConsumer() {
//        DefaultMQPushConsumer consumer = getDefaultBatchConsumer();
//        consumer.registerMessageListener(imageMessageDBConsumer);
//        consumer.setConsumerGroup("image-db");
//        try {
//            consumer.subscribe("image-message", "");
//            consumer.start();
//        } catch (Exception e) {
//            log.info("******Exception：{}", e.getMessage());
//        }
//        return consumer;
//    }

//    private DefaultMQPushConsumer getDefaultBatchConsumer() {
//        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer();
//        consumer.setNamesrvAddr(nameServer);
//        consumer.setPullInterval(1000);
//        consumer.setConsumeThreadMax(2);
//        consumer.setConsumeThreadMin(1);
//        consumer.setPullBatchSize(16);
//        consumer.setConsumeMessageBatchMaxSize(16);
//        return consumer;
//    }

//    @Bean
//    @SneakyThrows
//    public TransactionMQProducer seckillTransactionMQProducer() {
//        TransactionMQProducer transactionMQProducer = new TransactionMQProducer();
//        transactionMQProducer.setNamesrvAddr(nameServer);
//        transactionMQProducer.setNamespace("seckill-service");
//        transactionMQProducer.setProducerGroup("order-producer");
//        transactionMQProducer.setInstanceName("SeckillTransactionMQProducer");
//        transactionMQProducer.setRetryTimesWhenSendFailed(1);
//        transactionMQProducer.start();
//        return transactionMQProducer;
//    }

}
