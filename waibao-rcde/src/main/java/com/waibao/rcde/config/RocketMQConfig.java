//package com.waibao.rcde.config;
//
//import lombok.RequiredArgsConstructor;
//import lombok.SneakyThrows;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
//import org.apache.rocketmq.client.producer.DefaultMQProducer;
//import org.apache.rocketmq.spring.autoconfigure.RocketMQProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
///**
// * RocketMQConfig
// *
// * @author alexpetertyler
// * @since 2022-02-17
// */
//@Slf4j
//@Configuration
//@RequiredArgsConstructor
//public class RocketMQConfig {
//    private final RocketMQProperties rocketMQProperties;
//
//    @Bean
//    @SneakyThrows
//    public DefaultMQProducer orderCreateMQProducer() {
//        DefaultMQProducer orderCreate = new DefaultMQProducer("orderCreate");
//        orderCreate.setNamesrvAddr(rocketMQProperties.getNameServer());
//        orderCreate.start();
//        return orderCreate;
//    }
//
//    @Bean
//    @SneakyThrows
//    public DefaultMQProducer orderCompensationMQProducer() {
//        DefaultMQProducer seckillDelay = new DefaultMQProducer("orderCompensation");
//        seckillDelay.setNamesrvAddr(rocketMQProperties.getNameServer());
//        seckillDelay.start();
//        return seckillDelay;
//    }
//
//    private DefaultMQPushConsumer getSingleThreadBatchConsumer() {
//        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer();
//        consumer.setNamesrvAddr(rocketMQProperties.getNameServer());
//        consumer.setPullInterval(1000);
//        consumer.setConsumeThreadMax(1);
//        consumer.setConsumeThreadMin(1);
//        consumer.setPullBatchSize(760);
//        consumer.setMaxReconsumeTimes(3);
//        consumer.setConsumeMessageBatchMaxSize(760);
//        return consumer;
//    }
//}
