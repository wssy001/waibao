package com.waibao.order.service.mq;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.order.entity.MqMsgCompensation;
import com.waibao.order.entity.OrderRetailer;
import com.waibao.order.entity.OrderUser;
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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * OrderCreateConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCreateConsumer implements MessageListenerConcurrently {
    private final AsyncService asyncService;
    private final OrderUserService orderUserService;
    private final OrderRetailerService orderRetailerService;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;

    @Override
    @SneakyThrows
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        List<OrderUser> orderUsers = convert(msgs);
        Map<String, OrderUser> orderUserMap = new ConcurrentHashMap<>();
        Map<String, OrderRetailer> orderRetailerMap = new ConcurrentHashMap<>();
        orderUsers.parallelStream()
                .forEach(orderUser -> {
                    orderUserMap.put(orderUser.getOrderId(), orderUser);
                    orderRetailerMap.put(orderUser.getOrderId(), BeanUtil.copyProperties(orderUser, OrderRetailer.class));
                });

        asyncService.basicTask(() -> orderUserService.saveBatch(orderUserMap.values()));
        asyncService.basicTask(() -> orderRetailerService.saveBatch(orderRetailerMap.values()));
        asyncService.basicTask(() -> mqMsgCompensationMapper.update(null,
                Wrappers.<MqMsgCompensation>lambdaUpdate()
                        .in(MqMsgCompensation::getMsgId, orderUsers.parallelStream()
                                .map(OrderUser::getOrderId)
                                .collect(Collectors.toList()))
                        .set(MqMsgCompensation::getStatus, "补偿消息已消费")));

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private List<OrderUser> convert(Collection<MessageExt> msgs) {
        return msgs.parallelStream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody()), OrderUser.class))
                .collect(Collectors.toList());
    }

}
