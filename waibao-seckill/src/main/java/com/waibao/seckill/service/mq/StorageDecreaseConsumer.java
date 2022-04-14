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
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
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
    private final AsyncMQMessage asyncMQMessage;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final SeckillGoodsCacheService seckillGoodsCacheService;

    private DefaultMQProducer orderUpdateMQProducer;
    private DefaultMQProducer orderCancelMQProducer;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        log.info("******StorageDecreaseConsumer：本轮收到消息：{}", msgs.size());
        Map<String, MessageExt> messageExtMap = msgs.parallelStream()
                .collect(Collectors.toMap(MessageExt::getMsgId, Function.identity()));
        log.info("******StorageDecreaseConsumer：处理后消息数量：{}", messageExtMap.size());
        ConcurrentMap<Long, List<OrderVO>> collect = messageExtMap.values()
                .parallelStream()
                .map(messageExt -> (OrderVO) JSON.parseObject(messageExt.getBody(), OrderVO.class))
                .collect(Collectors.groupingByConcurrent(OrderVO::getGoodsId));

        List<OrderVO> cancel = new ArrayList<>();
        List<OrderVO> complete = new ArrayList<>();

        collect.forEach((k, v) -> {
            int trueStorage = seckillGoodsMapper.selectTrueStorage(k);
            if (trueStorage == 0) {
                log.error("******StorageDecreaseConsumer：goodsId：{} 商品售罄", k);
                cancel.addAll(v);
                seckillGoodsCacheService.updateGoodsStatus(k, false);
                return;
            }

            v.sort(Comparator.comparingLong(orderVO -> orderVO.getPurchaseTime().getTime()));
            int totalCount = v.parallelStream()
                    .mapToInt(OrderVO::getCount)
                    .sum();
            int update = seckillGoodsMapper.decreaseStorage(k, totalCount);
            if (update == 1) {
                complete.addAll(v);
                log.info("******StorageDecreaseConsumer：goodsId{} 已批量扣减库存 {}个", k, v.size());
            } else {
                log.info("******StorageDecreaseConsumer：goodsId：{} 库存告急，单个扣减", k);
                seckillGoodsCacheService.updateGoodsStatus(k, false);
                for (OrderVO orderVO : v) {
                    update = seckillGoodsMapper.decreaseStorage(k, orderVO.getCount());
                    if (update == 0) {
                        cancel.add(orderVO);
                    } else {
                        complete.add(orderVO);
                    }
                }
            }
        });

        if (!complete.isEmpty()) asyncMQMessage.sendMessage(orderUpdateMQProducer, complete.stream()
                .peek(orderVO -> orderVO.setStatus("购买成功"))
                .peek(orderVO -> log.info("******StorageDecreaseConsumer：userId：{},orderId：{} 购买成功", orderVO.getUserId(), orderVO.getOrderId()))
                .map(orderVO -> new Message("order", "update", orderVO.getOrderId(), JSON.toJSONBytes(orderVO)))
                .collect(Collectors.toList())
        );

        if (!cancel.isEmpty()) asyncMQMessage.sendMessage(orderCancelMQProducer, cancel.stream()
                .peek(orderVO -> orderVO.setStatus("购买失败，库存不足"))
                .peek(orderVO -> log.info("******StorageDecreaseConsumer：userId：{},orderId：{} 购买失败 原因：库存不足", orderVO.getUserId(), orderVO.getOrderId()))
                .map(orderVO -> new Message("order", "cancel", orderVO.getOrderId(), JSON.toJSONBytes(orderVO)))
                .collect(Collectors.toList())
        );

        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    @Lazy
    @Autowired
    public void setOrderUpdateMQProducer(DefaultMQProducer orderUpdateMQProducer) {
        this.orderUpdateMQProducer = orderUpdateMQProducer;
    }

    @Lazy
    @Autowired
    public void setOrderCancelMQProducer(DefaultMQProducer orderCancelMQProducer) {
        this.orderCancelMQProducer = orderCancelMQProducer;
    }
}
