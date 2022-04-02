package com.waibao.seckill.service.mq;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.waibao.seckill.entity.SeckillGoods;
import com.waibao.seckill.mapper.SeckillGoodsMapper;
import com.waibao.seckill.service.cache.SeckillGoodsCacheService;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * StorageDecreaseConsumer
 *
 * @author alexpetertyler
 * @since 2022/3/18
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StorageDecreaseConsumer implements MessageListenerConcurrently {
    private AsyncMQMessage asyncMQMessage;
    private SeckillGoodsCacheService goodsCacheService;
    private SeckillGoodsMapper seckillGoodsMapper;
    private DefaultMQProducer orderUpdateMQProducer;
    private DefaultMQProducer orderCancelMQProducer;

    @Autowired
    public StorageDecreaseConsumer(AsyncMQMessage asyncMQMessage, SeckillGoodsCacheService goodsCacheService, SeckillGoodsMapper seckillGoodsMapper, @Lazy DefaultMQProducer orderUpdateMQProducer, @Lazy DefaultMQProducer orderCancelMQProducer) {
        this.asyncMQMessage = asyncMQMessage;
        this.goodsCacheService = goodsCacheService;
        this.seckillGoodsMapper = seckillGoodsMapper;
        this.orderUpdateMQProducer = orderUpdateMQProducer;
        this.orderCancelMQProducer = orderCancelMQProducer;
    }

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        Map<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getKeys(), messageExt));

        ConcurrentMap<Long, List<OrderVO>> collect = messageExtMap.values()
                .parallelStream()
                .map(messageExt -> JSON.parseObject(new String(messageExt.getBody()), OrderVO.class))
                .collect(Collectors.groupingByConcurrent(OrderVO::getGoodsId));

        List<OrderVO> cancel = new ArrayList<>();
        List<OrderVO> complete = new ArrayList<>();

        collect.forEach((k, v) -> {
            v.sort(Comparator.comparingLong(orderVO -> orderVO.getPurchaseTime().getTime()));
            int totalCount = v.parallelStream()
                    .mapToInt(OrderVO::getCount)
                    .sum();
            SeckillGoods seckillGoods = seckillGoodsMapper.selectOne(Wrappers.<SeckillGoods>lambdaQuery().eq(SeckillGoods::getGoodsId, k));
            Integer currentStorage = seckillGoods.getStorage();
            if (currentStorage >= totalCount) {
                seckillGoodsMapper.update(null, Wrappers.<SeckillGoods>lambdaUpdate()
                        .eq(SeckillGoods::getGoodsId, k)
                        .eq(SeckillGoods::getStorage, currentStorage)
                        .set(SeckillGoods::getStorage, currentStorage - totalCount)
                );
                complete.addAll(v);
            } else {
                goodsCacheService.updateGoodsStatus(k, true);
                for (OrderVO orderVO : v) {
                    int update = seckillGoodsMapper.update(null, Wrappers.<SeckillGoods>lambdaUpdate()
                            .eq(SeckillGoods::getGoodsId, k)
                            .ge(SeckillGoods::getStorage, orderVO.getCount())
                            .set(SeckillGoods::getStorage, currentStorage - orderVO.getCount())
                    );
                    if (update == 0) {
                        cancel.add(orderVO);
                    } else {
                        complete.add(orderVO);
                    }
                }
            }
        });

        asyncMQMessage.sendMessage(orderUpdateMQProducer, complete.parallelStream()
                .peek(orderVO -> orderVO.setStatus("购买成功"))
                .map(orderVO -> new Message("order", "update", orderVO.getOrderId(), JSON.toJSONBytes(orderVO)))
                .collect(Collectors.toList())
        );

        asyncMQMessage.sendMessage(orderCancelMQProducer, cancel.parallelStream()
                .peek(orderVO -> orderVO.setStatus("购买失败，库存不足"))
                .map(orderVO -> new Message("order", "cancel", orderVO.getOrderId(), JSON.toJSONBytes(orderVO)))
                .collect(Collectors.toList())
        );

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

}
