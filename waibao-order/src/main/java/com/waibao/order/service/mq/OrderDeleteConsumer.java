package com.waibao.order.service.mq;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.order.entity.MqMsgCompensation;
import com.waibao.order.mapper.MqMsgCompensationMapper;
import com.waibao.order.service.db.OrderRetailerService;
import com.waibao.order.service.db.OrderUserService;
import com.waibao.util.async.AsyncService;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * OrderDeleteConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderDeleteConsumer implements MessageListenerConcurrently {
    private final AsyncService asyncService;
    private final OrderUserService orderUserService;
    private final OrderRetailerService orderRetailerService;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;

    @Override
    @SneakyThrows
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        Set<String> orderIdList = convert(msgs);
        asyncService.basicTask(() -> orderUserService.removeByIds(orderIdList));
        asyncService.basicTask(() -> orderRetailerService.removeByIds(orderIdList));
        asyncService.basicTask(() -> mqMsgCompensationMapper.update(null,
                Wrappers.<MqMsgCompensation>lambdaUpdate()
                        .in(MqMsgCompensation::getMsgId, orderIdList)
                        .set(MqMsgCompensation::getStatus, "补偿消息已消费")));
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private Set<String> convert(List<MessageExt> msgs) {
        return msgs.parallelStream()
                .map(MessageExt::getKeys)
                .collect(Collectors.toSet());
    }

}
