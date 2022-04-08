package com.waibao.order.controller;

import com.waibao.order.entity.OrderRetailer;
import com.waibao.order.service.cache.OrderRetailerCacheService;
import com.waibao.util.vo.GlobalResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OrderRetailerController
 *
 * @author alexpetertyler
 * @since 2022/4/5
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/retailer/order")
public class OrderRetailerController {
    private final OrderRetailerCacheService orderRetailerCacheService;

    @GetMapping(value = "/info", produces = MediaType.APPLICATION_JSON_VALUE)
    public GlobalResult<OrderRetailer> getOrderInfo(
            @RequestParam("retailerId") Long retailerId,
            @RequestParam("orderId") String orderId
    ) {
        OrderRetailer orderRetailer = orderRetailerCacheService.get(retailerId, orderId);
        if (orderRetailer == null) return GlobalResult.error("无法查询到该订单");
        return GlobalResult.success(orderRetailer);
    }

}
