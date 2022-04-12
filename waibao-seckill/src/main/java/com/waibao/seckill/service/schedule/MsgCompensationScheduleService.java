package com.waibao.seckill.service.schedule;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.seckill.entity.MqMsgCompensation;
import com.waibao.seckill.service.db.MqMsgCompensationService;
import com.waibao.util.async.AsyncService;
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
 * MsgCompensationScheduleService
 *
 * @author alexpetertyler
 * @since 2022/3/3
 */
@Slf4j
//@Service
@RequiredArgsConstructor
public class MsgCompensationScheduleService {
    private final AsyncService asyncService;
    private final MqMsgCompensationService mqMsgCompensationService;
    private final DefaultMQProducer seckillCompensationMQProducer;

    @SneakyThrows
    @Scheduled(cron = "*/10 * * * * ?")
    public void reSend() {
        List<MqMsgCompensation> collect = mqMsgCompensationService.list(Wrappers.<MqMsgCompensation>lambdaQuery().eq(MqMsgCompensation::getTopic, "rcde")
                .eq(MqMsgCompensation::getStatus, "补偿消息未发送")
                .eq(MqMsgCompensation::getTopic, "rcde"))
                .parallelStream()
                .peek(mqMsgCompensation -> mqMsgCompensation.setStatus("补偿消息已发送"))
                .collect(Collectors.toList());

        if (collect.isEmpty()) return;
        log.info("******MsgCompensationScheduleService：{}", "开始准备发送补偿消息");
        List<Message> messageList = collect.parallelStream()
                .map(mqMsgCompensation -> new Message("rcde", mqMsgCompensation.getTags(), mqMsgCompensation.getMsgId(), mqMsgCompensation.getContent().getBytes()))
                .collect(Collectors.toList());

        asyncService.basicTask(() -> {
            try {
                seckillCompensationMQProducer.send(messageList, (SendCallback) null);
            } catch (Exception ignored) {
            }
        });
        asyncService.basicTask(() -> mqMsgCompensationService.updateBatchById(collect));
        log.info("******MsgCompensationScheduleService：{}", "补偿消息已发送");
    }

}
