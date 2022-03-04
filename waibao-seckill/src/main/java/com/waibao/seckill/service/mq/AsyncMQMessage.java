package com.waibao.seckill.service.mq;

import com.waibao.seckill.entity.MqMsgCompensation;
import com.waibao.seckill.service.db.MqMsgCompensationService;
import com.waibao.util.mq.SelectSingleMessageQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.common.message.Message;
import org.springframework.stereotype.Service;

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
            log.info("******AsyncMQMessage.sendMessage：补偿消息存储成功，消息id：{}", msgId);
        } catch (Exception e) {
            log.info("******AsyncMQMessage.sendMessage：补偿消息存储失败，消息id：{}", msgId);
        }
    }
}
