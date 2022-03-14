package com.waibao.seckill.service.mq;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.seckill.entity.MqMsgCompensation;
import com.waibao.seckill.mapper.MqMsgCompensationMapper;
import com.waibao.seckill.service.cache.GoodsCacheService;
import com.waibao.seckill.service.cache.LogSeckillGoodsCacheService;
import com.waibao.seckill.service.cache.PurchasedUserCacheService;
import com.waibao.util.async.AsyncService;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
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
    private final GoodsCacheService goodsCacheService;
    private final MqMsgCompensationMapper mqMsgCompensationMapper;
    private final PurchasedUserCacheService purchasedUserCacheService;
    private final LogSeckillGoodsCacheService logSeckillGoodsCacheService;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        Map<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getKeys(), messageExt));
        List<OrderVO> orderVOList = convert(messageExtMap.values());
        List<OrderVO> canceledList = logSeckillGoodsCacheService.batchCheckCancel(orderVOList);
        orderVOList.removeAll(canceledList);

        asyncService.basicTask(() -> goodsCacheService.batchRollBackStorage(orderVOList)
                .forEach(orderVO -> goodsStorageCacheServiceLog(orderVO, orderVOList)));
        asyncService.basicTask(() -> mqMsgCompensationMapper.update(null,
                Wrappers.<MqMsgCompensation>lambdaUpdate()
                        .in(MqMsgCompensation::getMsgId, orderVOList.parallelStream()
                                .map(OrderVO::getOrderId)
                                .collect(Collectors.toList()))
                        .set(MqMsgCompensation::getStatus, "补偿消息已消费")));
        asyncService.basicTask(() -> purchasedUserCacheService.decreaseBatch(orderVOList)
                .forEach(orderVO -> purchasedUserCacheServiceLog(orderVO, orderVOList)));
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    private void goodsStorageCacheServiceLog(OrderVO orderVO, List<OrderVO> orderVOList) {
        if (!orderVOList.contains(orderVO)) {
            log.info("******RedisStorageRollbackConsumer.goodsStorageCacheServiceLog：订单id：{} Redis库存回滚成功", orderVO.getOrderId());
        } else {
            log.error("******RedisStorageRollbackConsumer.goodsStorageCacheServiceLog：订单id：{} Redis库存回滚失败，原因：{} 处理：{}", orderVO.getOrderId(), "商品ID不存在", "人工处理");
        }
    }

    private void purchasedUserCacheServiceLog(OrderVO orderVO, List<OrderVO> orderVOList) {
        if (!orderVOList.contains(orderVO)) {
            log.info("******RedisStorageRollbackConsumer.purchasedUserCacheServiceLog：订单id：{} Redis用户已购买数回滚成功", orderVO.getOrderId());
        } else {
            log.error("******RedisStorageRollbackConsumer.purchasedUserCacheServiceLog：订单id：{} Redis用户已购买数回滚失败，原因：{} 处理：{}", orderVO.getOrderId(), "商品ID不存在", "人工处理");
        }
    }

    private List<OrderVO> convert(Collection<MessageExt> msgs) {
        return msgs.parallelStream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody()), OrderVO.class))
                .collect(Collectors.toList());
    }

}
