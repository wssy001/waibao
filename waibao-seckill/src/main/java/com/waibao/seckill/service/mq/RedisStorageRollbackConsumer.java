package com.waibao.seckill.service.mq;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.util.async.AsyncService;
import com.waibao.seckill.entity.MqMsgCompensation;
import com.waibao.seckill.mapper.MqMsgCompensationMapper;
import com.waibao.seckill.service.cache.PurchasedUserCacheService;
import com.waibao.seckill.service.cache.SeckillGoodsCacheService;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * StorageConsumer
 *
 * @author alexpetertyler
 * @since 2022/2/28
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisStorageRollbackConsumer implements MessageListenerConcurrently {
    private final AsyncService asyncService;
    private final SeckillGoodsCacheService goodsCacheService;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;
    private final PurchasedUserCacheService purchasedUserCacheService;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        Map<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getMsgId(), messageExt));
        List<OrderVO> orderVOList = new ArrayList<>(convert(messageExtMap.values()));
        if (orderVOList.isEmpty()) return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;

        asyncService.basicTask(() -> mqMsgCompensationMapper.update(null,
                Wrappers.<MqMsgCompensation>lambdaUpdate()
                        .in(MqMsgCompensation::getMsgId, orderVOList.parallelStream()
                                .map(OrderVO::getOrderId)
                                .collect(Collectors.toList()))
                        .set(MqMsgCompensation::getStatus, "补偿消息已消费")));
        asyncService.basicTask(() -> goodsStorageCacheServiceLog(goodsCacheService.batchRollBackStorage(orderVOList), orderVOList));
        asyncService.basicTask(() -> purchasedUserCacheServiceLog(purchasedUserCacheService.decreaseBatch(orderVOList), orderVOList));
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private void goodsStorageCacheServiceLog(List<OrderVO> failedList, List<OrderVO> orderVOList) {
        if (!failedList.isEmpty()) {
            orderVOList.removeIf(failedList::contains);
            failedList.forEach(orderVO -> log.error("******goodsStorageCacheServiceLog：订单id：{} Redis库存回滚失败，原因：{} 处理：{}", orderVO.getOrderId(), "商品ID不存在", "人工处理"));
        }

        orderVOList.forEach(orderVO -> log.error("******goodsStorageCacheServiceLog：订单id：{} Redis库存回滚成功", orderVO.getOrderId()));
    }

    private void purchasedUserCacheServiceLog(List<OrderVO> failedList, List<OrderVO> orderVOList) {
        if (!failedList.isEmpty()) {
            orderVOList.removeIf(failedList::contains);
            failedList.forEach(orderVO -> log.error("******RedisStorageRollbackConsumer.purchasedUserCacheServiceLog：订单id：{} 用户已购买数回滚失败，原因：{} 处理：{}", orderVO.getOrderId(), "商品ID不存在", "人工处理"));
        }

        orderVOList.forEach(orderVO -> log.info("******RedisStorageRollbackConsumer.purchasedUserCacheServiceLog：订单id：{} 用户已购买数回滚成功", orderVO.getOrderId()));
    }

    private List<OrderVO> convert(Collection<MessageExt> msgs) {
        return msgs.parallelStream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody()), OrderVO.class))
                .collect(Collectors.toList());
    }

}
