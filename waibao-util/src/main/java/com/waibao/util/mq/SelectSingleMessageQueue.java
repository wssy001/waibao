package com.waibao.util.mq;

import lombok.SneakyThrows;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.MessageQueue;

/**
 * SelectSingleMessageQueue
 *
 * @author alexpetertyler
 * @since 2022/3/3
 */
public class SelectSingleMessageQueue {
    public static MessageQueue selectFirst(DefaultMQProducer defaultMQProducer, String topic) {
        return select(defaultMQProducer, topic, 0);
    }

    @SneakyThrows
    public static MessageQueue select(DefaultMQProducer defaultMQProducer, String topic, int index) {
        return defaultMQProducer.fetchPublishMessageQueues(topic)
                .get(index);
    }
}
