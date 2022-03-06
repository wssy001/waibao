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
        return selectCreate(defaultMQProducer, topic);
    }

    public static MessageQueue selectCreate(DefaultMQProducer defaultMQProducer, String topic) {
        return select(defaultMQProducer, topic, 0);
    }

    public static MessageQueue selectUpdate(DefaultMQProducer defaultMQProducer, String topic) {
        return select(defaultMQProducer, topic, 1);
    }

    public static MessageQueue selectCancel(DefaultMQProducer defaultMQProducer, String topic) {
        return select(defaultMQProducer, topic, 2);
    }

    public static MessageQueue selectDelete(DefaultMQProducer defaultMQProducer, String topic) {
        return select(defaultMQProducer, topic, 3);
    }

    @SneakyThrows
    public static MessageQueue select(DefaultMQProducer defaultMQProducer, String topic, int index) {
        return defaultMQProducer.fetchPublishMessageQueues(topic)
                .get(index);
    }
}
