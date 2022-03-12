package com.waibao.rcde.service.mq;

import com.waibao.rcde.entity.MqMsgCompensation;
import com.waibao.rcde.service.db.MqMsgCompensationService;
import com.waibao.util.mq.SelectSingleMessageQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * SendAsyncMQMessage
 *
 * @author alexpetertyler
 * @since 2022/3/4
 */
@Slf4j
@Async
@Service
@RequiredArgsConstructor
public class AsyncMQMessage {
    private final Executor mqThreadPoolExecutor;
    private final MqMsgCompensationService mqMsgCompensationService;

    public void sendMessage(DefaultMQProducer producer, Message message) {
        Mono.fromFuture(CompletableFuture.supplyAsync(() -> {
                    try {
                        return producer.send(message, SelectSingleMessageQueue.selectFirst(producer, message.getTopic()));
                    } catch (Exception e) {
                        return handleError(message, e.getMessage());
                    }
                }
                , mqThreadPoolExecutor))
                .filter(Objects::nonNull)
                .subscribe(sendResult -> executeSendResult(sendResult, message));
    }

    public void sendMessage(DefaultMQProducer producer, Map<String, List<Message>> map) {
        Flux.fromIterable(map.values())
                .subscribe(sameTopicList -> {
                    String topic = sameTopicList.get(0).getTopic();
                    Mono.fromFuture(CompletableFuture.supplyAsync(() -> {
                                try {
                                    return producer.send(sameTopicList, SelectSingleMessageQueue.selectFirst(producer, topic));
                                } catch (Exception e) {
                                    return handleError(sameTopicList, e.getMessage());
                                }
                            }
                            , mqThreadPoolExecutor))
                            .filter(Objects::nonNull)
                            .subscribe(sendResult -> executeSendResult(sendResult, sameTopicList));
                });
    }

    private SendResult handleError(List<Message> messageList, String errorMessage) {
        messageList.parallelStream()
                .forEach(message -> handleError(message, errorMessage));
        return null;
    }

    private SendResult handleError(Message message, String errorMessage) {
        log.error("******AsyncMQMessage.sendMessage：消息id：{} 原因：{} 处理:{}", message.getKeys(), "producer发送失败", "等待延迟补偿结果");
        sendMqMsgCompensation(message, errorMessage);
        return null;
    }

    private void executeSendResult(SendResult sendResult, Message message) {
        if (!sendResult.getSendStatus().equals(SendStatus.SEND_OK)) {
            String name = sendResult.getSendStatus().name();
            log.error("******AsyncMQMessage.sendMessage：消息id：{} 原因：{} 处理:{}", message.getKeys(), name, "等待延迟补偿结果");
            sendMqMsgCompensation(message, name);
        } else {
            log.info("******AsyncMQMessage.sendMessage：消息id：{} 发送成功", message.getKeys());
        }
    }

    private void executeSendResult(SendResult sendResult, List<Message> messages) {
        if (!sendResult.getSendStatus().equals(SendStatus.SEND_OK)) {
            String name = sendResult.getSendStatus().name();
            sendMqMsgCompensation(messages, name);
            messages.parallelStream()
                    .forEach(message -> log.error("******AsyncMQMessage.sendMessage：消息id：{} 原因：{} 处理:{}", message.getKeys(), name, "等待延迟补偿结果"));
        } else {
            messages.parallelStream()
                    .forEach(message -> log.info("******AsyncMQMessage.sendMessage：消息id：{} 发送成功", message.getKeys()));
        }
    }

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

    private void sendMqMsgCompensation(Message message, String exceptionMsg) {
        String msgId = message.getKeys();
        MqMsgCompensation mqMsgCompensation = new MqMsgCompensation()
                .setTags(message.getTags())
                .setTopic(message.getTopic())
                .setContent(new String(message.getBody()))
                .setMsgId(msgId)
                .setExceptionMsg(exceptionMsg);
        try {
            mqMsgCompensationService.saveOrUpdate(mqMsgCompensation);
            log.info("******AsyncMQMessage.sendMqMsgCompensation：补偿消息存储成功，消息id：{}", msgId);
        } catch (Exception e) {
            log.info("******AsyncMQMessage.sendMqMsgCompensation：补偿消息存储失败，消息id：{}", msgId);
        }
    }
}
