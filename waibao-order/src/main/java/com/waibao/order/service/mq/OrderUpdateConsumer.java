package com.waibao.order.service.mq;

import com.alibaba.fastjson.JSON;
import com.waibao.order.entity.OrderRetailer;
import com.waibao.order.entity.OrderUser;
import com.waibao.order.service.cache.OrderGoodsCacheService;
import com.waibao.order.service.db.OrderRetailerService;
import com.waibao.order.service.db.OrderUserService;
import com.waibao.util.tools.BeanUtil;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * OrderUpdateConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderUpdateConsumer implements MessageListenerConcurrently {
    private final OrderGoodsCacheService orderGoodsCacheService;
    private final OrderRetailerService orderRetailerService;
    private final OrderUserService orderUserService;

    @Override
    @SneakyThrows
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        List<OrderVO> orderVOs = convert(msgs);
        List<OrderVO> failUpdateOrderVOs = orderGoodsCacheService.updateBatch(orderVOs);
        orderVOs.removeAll(failUpdateOrderVOs);

        Future<List<OrderRetailer>> orderRetailerFuture = convertAsync(orderVOs, OrderRetailer.class);
        Future<List<OrderUser>> orderUserFuture = convertAsync(orderVOs, OrderUser.class);
        while (true) {
            if (orderRetailerFuture.isDone() && orderUserFuture.isDone()) break;
        }
        List<OrderRetailer> orderRetailers = orderRetailerFuture.get();
        List<OrderUser> orderUsers = orderUserFuture.get();
        if (!orderUsers.isEmpty()) new Thread(() -> orderUserService.updateBatchById(orderUsers)).start();
        if (!orderRetailers.isEmpty()) new Thread(() -> orderRetailerService.updateBatchById(orderRetailers)).start();
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private List<OrderVO> convert(List<MessageExt> msgs) {
        return msgs.parallelStream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody()), OrderVO.class))
                .collect(Collectors.toList());
    }

    private <T> Future<List<T>> convertAsync(List<OrderVO> msgs, Class<T> clazz) {
        return new AsyncResult<>(msgs.parallelStream()
                .map(orderVO -> BeanUtil.copyProperties(orderVO, clazz))
                .collect(Collectors.toList()));
    }
}
