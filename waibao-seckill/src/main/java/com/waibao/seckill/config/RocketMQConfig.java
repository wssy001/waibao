package com.waibao.seckill.config;

import com.waibao.seckill.service.mq.SeckillTransactionListener;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
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
    private final SeckillTransactionListener seckillTransactionListener;

    @Bean
    @SneakyThrows
    public DefaultMQProducer seckillTransactionMQProducer() {
        TransactionMQProducer transactionMQProducer = new TransactionMQProducer("seckillTransaction");
        transactionMQProducer.setNamesrvAddr(rocketMQProperties.getNameServer());
        transactionMQProducer.setTransactionListener(seckillTransactionListener);
        transactionMQProducer.start();
        return transactionMQProducer;
    }

    @Bean
    @SneakyThrows
    public DefaultMQProducer seckillDelayMQProducer() {
        DefaultMQProducer seckillDelay = new DefaultMQProducer("seckillDelay");
        seckillDelay.setNamesrvAddr(rocketMQProperties.getNameServer());
        seckillDelay.start();
        return seckillDelay;
    }
}
