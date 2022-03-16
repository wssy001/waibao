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
    Captcha(0, "验证码"),
    LogOrder(1, "订单日志"),
    LogPayment(2, "支付日志"),
    LogStorage(3, "库存日志"),
    GoodsRetailer(4, "商品|商户"),
    Goods(5, "商品"),
    OrderRetailer(6, "订单|商户"),
    OrderUser(7, "订单|用户"),
    OrderGoods(8, "订单|商品"),
    Payment(9, "支付"),
    UserCredit(10, "用户积分"),
    User(11, "用户"),
    Admin(12, "管理员"),
    Transaction(13, "事务"),
    Storage(14, "商品库存"),
    Deposit(15, "还款记录"),
    Rule(15, "规则"),
    RiskUser(15, "风险用户");

    private int index;
    private String desc;
}
