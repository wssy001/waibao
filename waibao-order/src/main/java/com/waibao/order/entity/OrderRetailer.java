package com.waibao.order.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

/**
 * <p>
 *
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-02-28
 */
@Getter
@Setter
@Accessors(chain = true)
public class OrderRetailer extends Model<OrderRetailer> {

    /**
     * 订单ID
     */
    @TableId("order_id")
    private String orderId;

    /**
     * 商品ID
     */
    @TableField("goods_id")
    private Long goodsId;

    /**
     * 购买者ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 卖家ID
     */
    @TableField("retailer_id")
    private Long retailerId;

    /**
     * 商品金额
     */
    @TableField("goods_price")
    private BigDecimal goodsPrice;

    /**
     * 订单金额
     */
    @TableField("order_price")
    private BigDecimal orderPrice;

    /**
     * 购买时间
     */
    @TableField("purchase_time")
    private Date purchaseTime;

    /**
     * 是否支付
     */
    @TableField("paid")
    private Boolean paid;

    /**
     * 状态
     */
    @TableField("`status`")
    private String status;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /**
     * 逻辑删除
     */
    private Boolean enable;

    @Override
    public Serializable pkVal() {
        return this.orderId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderRetailer)) return false;
        OrderRetailer that = (OrderRetailer) o;
        return getOrderId().equals(that.getOrderId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getOrderId());
    }
}
