package com.waibao.rcde.config;

import com.waibao.rcde.service.mq.CheckRiskUserConsumer;
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
    private final CheckRiskUserConsumer checkRiskUserConsumer;
    private final RocketMQProperties rocketMQProperties;

    @Bean
    @SneakyThrows
    public DefaultMQProducer riskUserCheckMQProducer() {
        DefaultMQProducer riskUserCheck = new DefaultMQProducer("riskUserCheck");
        riskUserCheck.setNamesrvAddr(rocketMQProperties.getNameServer());
        riskUserCheck.start();
        return riskUserCheck;
    }

    @Bean
    @SneakyThrows
    public DefaultMQProducer rcdeCompensationMQProducer() {
        DefaultMQProducer rcdeCancel = new DefaultMQProducer("rcdeCompensation");
        rcdeCancel.setNamesrvAddr(rocketMQProperties.getNameServer());
        rcdeCancel.start();
        return rcdeCancel;
    }

    @Bean
    @SneakyThrows
    public DefaultMQPushConsumer riskUserCheckBatchConsumer() {
        DefaultMQPushConsumer consumer = getSingleThreadBatchConsumer();
        consumer.registerMessageListener(checkRiskUserConsumer);
        consumer.setConsumerGroup("riskUserCheck");
        consumer.subscribe("riskUser", "check");
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
