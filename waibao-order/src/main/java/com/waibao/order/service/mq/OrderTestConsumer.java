package com.waibao.order.service.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
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
import com.waibao.util.vo.order.OrderVO;
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
public class OrderTestConsumer implements MessageListenerConcurrently {
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
                .forEach(messageExt -> messageExtMap.put(messageExt.getMsgId(), messageExt));
        List<OrderVO> orderVOList = logOrderGoodsCacheService.batchCheckNotConsumedTags(convert(messageExtMap.values(), OrderVO.class), "create");

        Future<List<OrderUser>> orderUsersFuture = asyncService.basicTask(convert(orderVOList, OrderUser.class));
        Future<List<OrderRetailer>> orderRetailersFuture = asyncService.basicTask(convert(orderVOList, OrderRetailer.class));
        Future<List<LogOrderGoods>> logOrderGoodsFuture = asyncService.basicTask(convert(orderVOList, LogOrderGoods.class));
        while (true) {
            if (orderRetailersFuture.isDone() && orderUsersFuture.isDone() && logOrderGoodsFuture.isDone()) break;
        }

        List<OrderUser> orderUsers = orderUsersFuture.get();
        List<LogOrderGoods> logOrderGoods = logOrderGoodsFuture.get();
        List<OrderRetailer> orderRetailers = orderRetailersFuture.get();

        asyncService.basicTask(() -> logOrderGoodsService.saveBatch(logOrderGoods));
        asyncService.basicTask(() -> orderUserService.saveOrUpdateBatch(orderUsers));
        asyncService.basicTask(() -> orderRetailerService.saveOrUpdateBatch(orderRetailers));
        asyncService.basicTask(() -> mqMsgCompensationMapper.update(null,
                Wrappers.<MqMsgCompensation>lambdaUpdate()
                        .in(MqMsgCompensation::getMsgId, msgs.stream().map(MessageExt::getMsgId).collect(Collectors.toList()))
                        .set(MqMsgCompensation::getStatus, "补偿消息已消费")));

        asyncService.basicTask(() -> orderUsers.forEach(orderUser -> log.info("******OrderTestConsumer：userId：{}，orderId：{} 订单创建成功", orderUser.getUserId(), orderUser.getOrderId())));
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private <T> List<T> convert(Collection<MessageExt> msgs, Class<T> clazz) {
        return msgs.parallelStream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody()), clazz))
                .collect(Collectors.toList());
    }

    private <T> List<T> convert(List<OrderVO> orderVOList, Class<T> clazz) {
        return orderVOList.parallelStream()
                .map(orderVO -> (JSONObject) JSON.toJSON(orderVO))
                .peek(jsonObject -> jsonObject.put("status", "订单创建"))
                .peek(jsonObject -> {
                    if (clazz == LogOrderGoods.class) {
                        jsonObject.put("topic", "order");
                        jsonObject.put("operation", "create");
                    }
                })
                .map(jsonObject -> jsonObject.toJavaObject(clazz))
                .collect(Collectors.toList());
    }

}
