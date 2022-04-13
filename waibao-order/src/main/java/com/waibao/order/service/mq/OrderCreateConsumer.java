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
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.function.Function;
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
        Map<String, MessageExt> messageExtMap = msgs.parallelStream()
                .collect(Collectors.toMap(Message::getKeys, Function.identity(), (prev, next) -> next));

        List<OrderUser> orderUsers = new CopyOnWriteArrayList<>();
        List<LogOrderGoods> logOrderGoods = new CopyOnWriteArrayList<>();
        List<OrderRetailer> orderRetailers = new CopyOnWriteArrayList<>();
        List<OrderVO> orderVOList = new CopyOnWriteArrayList<>(logOrderGoodsCacheService.batchCheckNotConsumedTags(convert(messageExtMap.values(), OrderVO.class), "create"));

        Future<Boolean> task = asyncService.basicTask(orderUsers.addAll(convert(orderVOList, OrderUser.class)));
        Future<Boolean> task2 = asyncService.basicTask(logOrderGoods.addAll(convert(orderVOList, LogOrderGoods.class)));
        Future<Boolean> task3 = asyncService.basicTask(orderRetailers.addAll(convert(orderVOList, OrderRetailer.class)));
        while (true) {
            if (task.isDone() && task2.isDone() && task3.isDone()) break;
        }

        task = asyncService.basicTask(orderUserService.saveBatch(orderUsers));
        task2 = asyncService.basicTask(logOrderGoodsService.saveBatch(logOrderGoods));
        task3 = asyncService.basicTask(orderRetailerService.saveBatch(orderRetailers));
        asyncService.basicTask(() -> mqMsgCompensationMapper.update(null,
                Wrappers.<MqMsgCompensation>lambdaUpdate()
                        .in(MqMsgCompensation::getMsgId, msgs.stream().map(MessageExt::getMsgId).collect(Collectors.toList()))
                        .set(MqMsgCompensation::getStatus, "补偿消息已消费")));

        while (true) {
            if (task.isDone() && task2.isDone() && task3.isDone()) break;
        }
        orderUsers.clear();
        logOrderGoods.clear();
        orderRetailers.clear();
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
