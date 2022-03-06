package com.waibao.order.controller;

import com.alibaba.fastjson.JSON;
import com.waibao.order.service.cache.OrderGoodsCacheService;
import com.waibao.order.service.mq.AsyncMQMessage;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.springframework.web.bind.annotation.*;

/**
 * OrderController
 *
 * @author alexpetertyler
 * @since 2022/3/1
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/order")
public class OrderController {
    private final AsyncMQMessage asyncMQMessage;
    private final DefaultMQProducer orderCancelMQProducer;
    private final DefaultMQProducer orderDeleteMQProducer;
    private final OrderGoodsCacheService orderGoodsCacheService;

    @GetMapping("/order/{orderId}")
    public GlobalResult<OrderVO> getOrderInfo(
            @PathVariable("orderId") String orderId,
            @RequestParam("userId") Long userId
    ) {
        OrderVO orderVO = getOrderId(orderId, userId);
        if (orderVO == null) return GlobalResult.error("订单ID无效");
        return GlobalResult.success(orderVO);
    }

    @PostMapping("/cancel/{orderId}")
    public GlobalResult<OrderVO> cancelOrder(
            @PathVariable("orderId") String orderId,
            @RequestParam("userId") Long userId
    ) {
        OrderVO orderVO = getOrderId(orderId, userId);
        if (orderVO == null) return GlobalResult.error("订单ID无效");
        Message message = new Message("order", "cancel", orderId, JSON.toJSONBytes(orderVO));
        asyncMQMessage.sendMessage(orderCancelMQProducer, message);
        return GlobalResult.success("订单取消请求提交成功", orderVO);
    }

    @PostMapping("/delete/{orderId}")
    public GlobalResult<OrderVO> deleteOrder(
            @PathVariable("orderId") String orderId,
            @RequestParam("userId") Long userId
    ) {
        OrderVO orderVO = getOrderId(orderId, userId);
        if (orderVO == null) return GlobalResult.error("订单ID无效");
        Message message = new Message("order", "delete", orderId, JSON.toJSONBytes(orderVO));
        asyncMQMessage.sendMessage(orderDeleteMQProducer, message);
        return GlobalResult.success("订单删除请求提交成功", orderVO);
    }

    private OrderVO getOrderId(String orderId, Long userId) {
        OrderVO orderVO = orderGoodsCacheService.get(orderId);
        if (orderVO == null || !orderVO.getUserId().equals(userId)) return null;
        return orderVO;
    }
}
