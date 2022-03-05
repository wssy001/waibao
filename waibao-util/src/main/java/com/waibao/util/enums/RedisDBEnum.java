package com.waibao.util.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * RedisDBEnum
 *
 * @author alexpetertyler
 * @since 2022/2/28
 */
@Getter
@AllArgsConstructor
public enum RedisDBEnum {
    Default(0, "默认"),
    Captcha(1, "验证码"),
    ShoppingCart(2, "购物车"),
    Order(3, "订单"),
    GoodsRetailer(4, "商品商户"),
    RetailerGoods(5, "商户商品"),
    OrderRetailer(6, "订单商户"),
    RetailerOrder(7, "商户订单"),
    User(8, "用户"),
    Admin(9, "管理员"),
    Transaction(10,"事务"),
    Storage(12, "商品库存表"),
    District(15, "中国省份城市数据库");

    private int index;
    private String desc;
}
