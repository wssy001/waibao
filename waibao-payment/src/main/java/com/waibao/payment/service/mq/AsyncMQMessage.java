package com.waibao.payment.service.mq;

import com.waibao.payment.entity.MqMsgCompensation;
import com.waibao.payment.service.db.MqMsgCompensationService;
import com.waibao.util.mq.SelectSingleMessageQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.*;
import org.apache.rocketmq.common.message.Message;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * SendAsyncMQMessage
 *
 * @author alexpetertyler
 * @since 2022/3/4
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncMQMessage {
    private final MqMsgCompensationService mqMsgCompensationService;

    @Async
    public void sendMessage(DefaultMQProducer producer, Message message) {
        String msgId = message.getKeys();
        SendResult send;
        try {
            send = producer.send(message, SelectSingleMessageQueue.selectFirst(producer, message.getTopic()));
        } catch (Exception e) {
            log.error("******AsyncMQMessage.sendMessage：消息id：{} 原因：{} 处理:{}", msgId, "producer发送失败", "等待延迟补偿结果");
            sendMqMsgCompensation(message, e.getMessage());
            return;
        }

        if (!send.getSendStatus().equals(SendStatus.SEND_OK)) {
            String name = send.getSendStatus().name();
            log.error("******AsyncMQMessage.sendMessage：消息id：{} 原因：{} 处理:{}", msgId, name, "等待延迟补偿结果");
            sendMqMsgCompensation(message, name);
        } else {
            log.info("******AsyncMQMessage.sendMessage：消息id：{} 发送成功", msgId);
        }
    }

    @Async
    //    1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
    public void sendDelayedMessage(DefaultMQProducer producer, Message message, int delayedLevel) {
        message.setTags(message.getTags() + "Check");
        message.setDelayTimeLevel(delayedLevel);
        String msgId = message.getKeys();
        SendResult send;
        try {
            send = producer.send(message, SelectSingleMessageQueue.selectFirst(producer, message.getTopic()));
        } catch (Exception e) {
            log.error("******AsyncMQMessage.sendDelayedMessage：消息id：{} 原因：{} 处理:{}", msgId, "producer发送失败", "等待延迟补偿结果");
            sendMqMsgCompensation(message, e.getMessage());
            return;
        }

        if (!send.getSendStatus().equals(SendStatus.SEND_OK)) {
            String name = send.getSendStatus().name();
            log.error("******AsyncMQMessage.sendDelayedMessage：消息id：{} 原因：{} 处理:{}", msgId, name, "等待延迟补偿结果");
            sendMqMsgCompensation(message, name);
        } else {
            log.info("******AsyncMQMessage.sendDelayedMessage：消息id：{} 发送成功", msgId);
        }
    }

    @Async
    public void sendMessageInTransaction(DefaultMQProducer producer, Message message) {
        String msgId = message.getKeys();
        TransactionSendResult send;
        try {
            send = producer.sendMessageInTransaction(message, null);
        } catch (Exception e) {
            log.error("******AsyncMQMessage.sendMessageInTransaction：消息id：{} 原因：{} 处理:{}", msgId, "producer发送失败", "等待延迟补偿结果");
            sendMqMsgCompensation(message, e.getMessage());
            return;
        }

        if (!send.getSendStatus().equals(SendStatus.SEND_OK)) {
            String name = send.getSendStatus().name();
            log.error("******AsyncMQMessage.sendMessageInTransaction：消息id：{} 原因：{} 处理:{}", msgId, name, "等待延迟补偿结果");
            sendMqMsgCompensation(message, name);
            return;
        }

        LocalTransactionState localTransactionState = send.getLocalTransactionState();
        if (localTransactionState.equals(LocalTransactionState.COMMIT_MESSAGE)) {
            log.info("******AsyncMQMessage.sendMessageInTransaction：消息id：{} 发送成功", msgId);
        } else if (localTransactionState.equals(LocalTransactionState.ROLLBACK_MESSAGE)) {
            log.info("******AsyncMQMessage.sendMessageInTransaction：消息id：{} 发送失败，处理：{}", msgId, "回滚");
        } else {
            log.info("******AsyncMQMessage.sendMessageInTransaction：消息id：{} 发送失败，处理：{}", msgId, "等待回查");
        }

    }

    @Async
    public void sendMessage(DefaultMQProducer producer, List<Message> messages) {
        messages.parallelStream()
                .collect(Collectors.groupingBy(Message::getTopic))
                .forEach((k, v) -> {
                    SendResult send;
                    try {
                        send = producer.send(v, SelectSingleMessageQueue.selectFirst(producer, k));
                    } catch (Exception e) {
                        sendMqMsgCompensation(v, e.getMessage());
                        v.parallelStream()
                                .forEach(message -> log.error("******AsyncMQMessage.sendMessage：消息id：{} 原因：{} 处理:{}", message.getKeys(), "producer发送失败", "等待延迟补偿结果"));
                        return;
                    }
                    if (!send.getSendStatus().equals(SendStatus.SEND_OK)) {
                        String name = send.getSendStatus().name();
                        sendMqMsgCompensation(v, name);
                        v.parallelStream()
                                .forEach(message -> log.error("******AsyncMQMessage.sendMessage：消息id：{} 原因：{} 处理:{}", message.getKeys(), name, "等待延迟补偿结果"));
                    } else {
                        v.parallelStream()
                                .forEach(message -> log.info("******AsyncMQMessage.sendMessage：消息id：{} 发送成功", message.getKeys()));
                    }
                });
    }

    @Async
    //    1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h
    public void sendDelayedMessage(DefaultMQProducer producer, List<Message> messages, int delayedLevel) {
        messages.parallelStream()
                .peek(message -> {
                    message.setTopic(message.getTopic() + "Check");
                    message.setDelayTimeLevel(delayedLevel);
                })
                .collect(Collectors.groupingBy(Message::getTopic))
                .forEach((k, v) -> {
                    SendResult send;
                    try {
                        send = producer.send(v, SelectSingleMessageQueue.selectFirst(producer, k));
                    } catch (Exception e) {
                        sendMqMsgCompensation(v, e.getMessage());
                        v.parallelStream()
                                .forEach(message -> log.error("******AsyncMQMessage.sendDelayedMessage：消息id：{} 原因：{} 处理:{}", message.getKeys(), "producer发送失败", "等待延迟补偿结果"));
                        return;
                    }
                    if (!send.getSendStatus().equals(SendStatus.SEND_OK)) {
                        String name = send.getSendStatus().name();
                        sendMqMsgCompensation(v, name);
                        v.parallelStream()
                                .forEach(message -> log.error("******AsyncMQMessage.sendDelayedMessage：消息id：{} 原因：{} 处理:{}", message.getKeys(), name, "等待延迟补偿结果"));
                    } else {
                        v.parallelStream()
                                .forEach(message -> log.info("******AsyncMQMessage.sendDelayedMessage：消息id：{} 发送成功", message.getKeys()));
                    }
                });
    }

    @Async
    private void sendMqMsgCompensation(List<Message> messages, String exceptionMsg) {
        List<MqMsgCompensation> collect = messages.parallelStream()
                .map(message -> {
                    MqMsgCompensation mqMsgCompensation = new MqMsgCompensation();
                    mqMsgCompensation.setTags(message.getTags());
                    mqMsgCompensation.setTopic(message.getTopic());
                    mqMsgCompensation.setContent(new String(message.getBody()));
                    String msgId = message.getKeys();
                    mqMsgCompensation.setMsgId(msgId);
                    mqMsgCompensation.setExceptionMsg(exceptionMsg);
                    return mqMsgCompensation;
                })
                .collect(Collectors.toList());
        try {
            mqMsgCompensationService.saveOrUpdateBatch(collect);
            messages.parallelStream()
                    .forEach(message -> log.info("******AsyncMQMessage.sendMqMsgCompensation：补偿消息存储成功，消息id：{}", message.getKeys()));
        } catch (Exception e) {
            messages.parallelStream()
                    .forEach(message -> log.info("******AsyncMQMessage.sendMqMsgCompensation：补偿消息存储失败，消息id：{}", message.getKeys()));
        }
    }

    @Async
    private void sendMqMsgCompensation(Message message, String exceptionMsg) {
        MqMsgCompensation mqMsgCompensation = new MqMsgCompensation();
        mqMsgCompensation.setTags(message.getTags());
        mqMsgCompensation.setTopic(message.getTopic());
        mqMsgCompensation.setContent(new String(message.getBody()));
        String msgId = message.getKeys();
        mqMsgCompensation.setMsgId(msgId);
        mqMsgCompensation.setExceptionMsg(exceptionMsg);
        try {
            mqMsgCompensationService.saveOrUpdate(mqMsgCompensation);
            log.info("******AsyncMQMessage.sendMqMsgCompensation：补偿消息存储成功，消息id：{}", msgId);
        } catch (Exception e) {
            log.info("******AsyncMQMessage.sendMqMsgCompensation：补偿消息存储失败，消息id：{}", msgId);
        }
    }
}
