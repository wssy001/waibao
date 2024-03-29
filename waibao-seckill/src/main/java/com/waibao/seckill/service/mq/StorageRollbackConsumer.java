package com.waibao.seckill.service.mq;

import com.alibaba.fastjson.JSON;
import com.waibao.seckill.mapper.SeckillGoodsMapper;
import com.waibao.seckill.service.cache.SeckillGoodsCacheService;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * StorageRollbackConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageRollbackConsumer implements MessageListenerConcurrently {
    private final SeckillGoodsCacheService goodsCacheService;
    private final SeckillGoodsMapper seckillGoodsMapper;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        Map<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getMsgId(), messageExt));

        ConcurrentMap<Long, List<OrderVO>> collect = messageExtMap.values()
                .parallelStream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody()), OrderVO.class))
                .collect(Collectors.groupingByConcurrent(OrderVO::getGoodsId));

        collect.forEach((k, v) -> {
            int trueStorage = seckillGoodsMapper.selectTrueStorage(k);
            if (!goodsCacheService.finished(k) && trueStorage == 0) {
                log.info("******StorageRollbackConsumer：goodsId：{} 重新开卖", k);
                goodsCacheService.updateGoodsStatus(k, true);
            }
            int totalCount = v.stream()
                    .mapToInt(OrderVO::getCount)
                    .sum();
            seckillGoodsMapper.increaseStorage(k, totalCount);
        });

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }
}
