package com.waibao.order.controller;

import com.waibao.order.service.cache.OrderGoodsCacheService;
import com.waibao.util.vo.GlobalResult;
import com.waibao.util.vo.order.OrderVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
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
    private final OrderGoodsCacheService orderGoodsCacheService;
    private final RocketMQTemplate rocketMQTemplate;

    @GetMapping("/order/{orderId}")
    public GlobalResult<OrderVO> getOrderInfo(
            @PathVariable("orderId") String orderId,
            @RequestParam("userId") Long userId
    ) {
        OrderVO orderVO = orderGoodsCacheService.get(orderId);
        if (orderVO == null || !orderVO.getUserId().equals(userId)) return GlobalResult.error("订单ID无效");
        return GlobalResult.success(orderVO);
    }

    @PostMapping("/cancel/{orderId}")
    public GlobalResult<Object> cancelOrder(
            @PathVariable("orderId") String orderId,
            @RequestParam("userId") Long userId
    ) {
        //todo  完成订单取消
        OrderVO orderVO = orderGoodsCacheService.get(orderId);
        if (orderVO == null || !orderVO.getUserId().equals(userId)) return GlobalResult.error("订单ID无效");
        rocketMQTemplate.convertAndSend();
    }
}
