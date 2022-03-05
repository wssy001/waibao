package com.waibao.order.service.mq;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.order.entity.MqMsgCompensation;
import com.waibao.order.entity.OrderRetailer;
import com.waibao.order.entity.OrderUser;
import com.waibao.order.mapper.MqMsgCompensationMapper;
import com.waibao.order.service.db.OrderRetailerService;
import com.waibao.order.service.db.OrderUserService;
import com.waibao.util.async.AsyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OrderRollbackConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelConsumer implements MessageListenerConcurrently {
    private final AsyncService asyncService;
    private final AsyncMQMessage asyncMQMessage;
    private final OrderUserService orderUserService;
    private final DefaultMQProducer orderCancelMQProducer;
    private final OrderRetailerService orderRetailerService;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        Set<String> orderIds = convert(msgs);

        asyncService.basicTask(() -> orderUserService.update(Wrappers.<OrderUser>lambdaUpdate().in(OrderUser::getOrderId, orderIds).set(OrderUser::getStatus, "订单取消")));
        asyncService.basicTask(() -> orderRetailerService.update(Wrappers.<OrderRetailer>lambdaUpdate().in(OrderRetailer::getOrderId, orderIds).set(OrderRetailer::getStatus, "订单取消")));
        asyncMQMessage.sendMessage(orderCancelMQProducer, msgs.parallelStream()
                .map(messageExt -> new Message("storage", "rollback", messageExt.getKeys(), messageExt.getBody()))
                .collect(Collectors.toList()));
        asyncService.basicTask(() -> mqMsgCompensationMapper.update(null,
                Wrappers.<MqMsgCompensation>lambdaUpdate()
                        .in(MqMsgCompensation::getMsgId, orderIds)
                        .set(MqMsgCompensation::getStatus, "补偿消息已消费")));
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private Set<String> convert(Collection<MessageExt> msgs) {
        return msgs.parallelStream()
                .map(MessageExt::getKeys)
                .collect(Collectors.toSet());
    }
}
