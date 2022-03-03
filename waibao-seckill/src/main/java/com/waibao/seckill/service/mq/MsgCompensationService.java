package com.waibao.seckill.service.mq;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.seckill.entity.MqMsgCompensation;
import com.waibao.seckill.service.db.MqMsgCompensationService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.common.message.Message;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * MsgCompensationService
 *
 * @author alexpetertyler
 * @since 2022/3/3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MsgCompensationService {
    private final MqMsgCompensationService mqMsgCompensationService;
    private final DefaultMQProducer orderCompensationMQProducer;

    @SneakyThrows
    @Scheduled(cron = "*/10 * * * * ?")
    public void orderCreate() {
        List<MqMsgCompensation> collect = mqMsgCompensationService.list(Wrappers.<MqMsgCompensation>lambdaQuery().eq(MqMsgCompensation::getTopic, "order")
                .eq(MqMsgCompensation::getStatus, "补偿消息未发送")
                .likeRight(MqMsgCompensation::getTags, "create"))
                .parallelStream()
                .peek(mqMsgCompensation -> mqMsgCompensation.setStatus("补偿消息已发送"))
                .collect(Collectors.toList());

        if (collect.isEmpty()) return;
        List<Message> messageList = collect.parallelStream()
                .map(mqMsgCompensation -> {
                    Message message = new Message("order", "create", mqMsgCompensation.getMsgId(), mqMsgCompensation.getContent().getBytes());
                    message.setTransactionId(mqMsgCompensation.getBusinessKey());
                    return message;
                }).collect(Collectors.toList());

        orderCompensationMQProducer.send(messageList, (SendCallback) null);
        mqMsgCompensationService.updateBatchById(collect);
    }
}
