package com.waibao.order.service.mq;

import com.alibaba.fastjson.JSON;
import com.waibao.order.entity.OrderRetailer;
import com.waibao.order.entity.OrderUser;
import com.waibao.order.service.cache.OrderGoodsCacheService;
import com.waibao.order.service.cache.OrderRetailerCacheService;
import com.waibao.order.service.cache.OrderUserCacheService;
import com.waibao.order.service.db.OrderRetailerService;
import com.waibao.order.service.db.OrderUserService;
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
 * OrderRollbackConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/1
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrderCancelConsumer implements MessageListenerConcurrently {
    private final OrderGoodsCacheService orderGoodsCacheService;
    private final OrderRetailerCacheService orderRetailerCacheService;
    private final OrderUserCacheService orderUserCacheService;
    private final OrderRetailerService orderRetailerService;
    private final OrderUserService orderUserService;

    @Override
    @SneakyThrows
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        //todo 完善逻辑，添加库存回滚
        Future<List<OrderRetailer>> orderRetailerFuture = convertAsync(msgs, OrderRetailer.class);
        Future<List<OrderUser>> orderUserFuture = convertAsync(msgs, OrderUser.class);
        Future<List<OrderVO>> orderVOFuture = convertAsync(msgs, OrderVO.class);
        while (true) {
            if (orderRetailerFuture.isDone() && orderUserFuture.isDone() && orderVOFuture.isDone()) break;
        }
        List<OrderRetailer> orderRetailers = orderRetailerFuture.get();
        List<OrderUser> orderUsers = orderUserFuture.get();
        List<OrderVO> orderVOs = orderVOFuture.get();

        orderGoodsCacheService.updateBatchAsync(orderVOs);
        orderRetailerFuture = orderRetailerCacheService.deleteBatchAsync(orderRetailers);
        orderUserFuture = orderUserCacheService.deleteBatchAsync(orderUsers);
        while (true) {
            if (orderRetailerFuture.isDone() && orderUserFuture.isDone()) break;
        }
        List<OrderRetailer> failInsertOrderRetailers = orderRetailerFuture.get();
        List<OrderUser> failInsertOrderUsers = orderUserFuture.get();

        orderUsers.removeAll(failInsertOrderUsers);
        orderRetailers.removeAll(failInsertOrderRetailers);
        if (!orderUsers.isEmpty()) new Thread(() -> orderUserService.removeByIds(orderUsers)).start();
        if (!orderRetailers.isEmpty()) new Thread(() -> orderRetailerService.removeByIds(orderRetailers)).start();
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private <T> Future<List<T>> convertAsync(List<MessageExt> msgs, Class<T> clazz) {
        return new AsyncResult<>(msgs.parallelStream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody()), clazz))
                .collect(Collectors.toList()));
    }
}
