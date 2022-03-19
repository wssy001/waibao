package com.waibao.order.service.mq;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.order.entity.LogOrderGoods;
import com.waibao.order.entity.MqMsgCompensation;
import com.waibao.order.entity.OrderRetailer;
import com.waibao.order.entity.OrderUser;
import com.waibao.order.mapper.MqMsgCompensationMapper;
import com.waibao.order.service.cache.LogOrderGoodsCacheService;
import com.waibao.order.service.db.LogOrderGoodsService;
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
import java.util.concurrent.Future;
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
    private final LogOrderGoodsService logOrderGoodsService;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;
    private final LogOrderGoodsCacheService logOrderGoodsCacheService;

    @Override
    @SneakyThrows
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        Map<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getKeys(), messageExt));
        convert(messageExtMap.values(), OrderUser.class)
                .parallelStream()
                .filter(orderUser -> logOrderGoodsCacheService.hasConsumedTags(orderUser.getGoodsId(), orderUser.getOrderId(), "create"))
                .map(OrderUser::getOrderId)
                .forEach(messageExtMap::remove);

        Future<List<OrderUser>> orderUsersFuture = asyncService.basicTask(convert(messageExtMap.values(), OrderUser.class));
        Future<List<OrderRetailer>> orderRetailersFuture = asyncService.basicTask(convert(messageExtMap.values(), OrderRetailer.class));
        Future<List<LogOrderGoods>> logOrderGoodsFuture = asyncService.basicTask(convert(messageExtMap.values(), LogOrderGoods.class));
        while (true) {
            if (orderRetailersFuture.isDone() && orderUsersFuture.isDone() && logOrderGoodsFuture.isDone()) break;
        }

        List<OrderUser> orderUsers = orderUsersFuture.get();
        List<OrderRetailer> orderRetailers = orderRetailersFuture.get();
        List<LogOrderGoods> logOrderGoods = logOrderGoodsFuture.get();

        asyncService.basicTask(() -> orderUserService.saveBatch(orderUsers));
        asyncService.basicTask(() -> logOrderGoodsService.saveBatch(logOrderGoods));
        asyncService.basicTask(() -> orderRetailerService.saveBatch(orderRetailers));
        asyncService.basicTask(() -> mqMsgCompensationMapper.update(null,
                Wrappers.<MqMsgCompensation>lambdaUpdate()
                        .in(MqMsgCompensation::getMsgId, messageExtMap.keySet())
                        .set(MqMsgCompensation::getStatus, "补偿消息已消费")));
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private <T> List<T> convert(Collection<MessageExt> msgs, Class<T> clazz) {
        return msgs.parallelStream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody())))
                .peek(jsonObject -> jsonObject.put("status", "订单创建"))
                .peek(jsonObject -> {
                    if (clazz == LogOrderGoods.class) jsonObject.put("topic", "create");
                })
                .map(jsonObject -> jsonObject.toJavaObject(clazz))
                .collect(Collectors.toList());
    }

}
