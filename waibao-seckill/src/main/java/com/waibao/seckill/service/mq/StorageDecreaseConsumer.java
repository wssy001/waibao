package com.waibao.seckill.service.mq;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
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
    private final AsyncMQMessage asyncMQMessage;
    private final SeckillGoodsMapper seckillGoodsMapper;
    private final SeckillGoodsCacheService goodsCacheService;

    private DefaultMQProducer orderUpdateMQProducer;
    private DefaultMQProducer orderCancelMQProducer;

    @Override
    public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs, ConsumeConcurrentlyContext context) {
        Map<String, MessageExt> messageExtMap = new ConcurrentHashMap<>();
        msgs.parallelStream()
                .forEach(messageExt -> messageExtMap.put(messageExt.getMsgId(), messageExt));

        //TODO 过滤付款成功的
        List<OrderVO> collect1 = messageExtMap.values()
                .parallelStream()
                .flatMap(messageExt -> JSONArray.parseArray(new String(messageExt.getBody()), OrderVO.class).stream())
                .collect(Collectors.toList());

        ConcurrentMap<Long, List<OrderVO>> collect = collect1.stream()
                .collect(Collectors.groupingByConcurrent(OrderVO::getGoodsId));

        List<OrderVO> cancel = new ArrayList<>();
        List<OrderVO> complete = new ArrayList<>();

        collect.forEach((k, v) -> {
            if (goodsCacheService.finished(k)) return;

            v.sort(Comparator.comparingLong(orderVO -> orderVO.getPurchaseTime().getTime()));
            int totalCount = v.parallelStream()
                    .mapToInt(OrderVO::getCount)
                    .sum();
            int update = seckillGoodsMapper.decreaseStorage(k, totalCount);
            if (update == 1) {
                complete.addAll(v);
            } else {
                log.info("******StorageRollbackConsumer：goodsId：{} 库存售罄，停止售卖", k);
                goodsCacheService.updateGoodsStatus(k, false);
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

        asyncMQMessage.sendMessage(orderUpdateMQProducer, complete.parallelStream()
                .peek(orderVO -> orderVO.setStatus("购买成功"))
                .peek(orderVO -> log.info("******StorageDecreaseConsumer：userId：{},orderId：{} 购买成功", orderVO.getUserId(), orderVO.getOrderId()))
                .map(orderVO -> new Message("order", "update", orderVO.getOrderId(), JSON.toJSONBytes(orderVO)))
                .collect(Collectors.toList())
        );

        asyncMQMessage.sendMessage(orderCancelMQProducer, cancel.parallelStream()
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
