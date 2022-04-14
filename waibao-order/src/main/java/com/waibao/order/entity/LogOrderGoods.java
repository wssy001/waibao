package com.waibao.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * <p>
 * 
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-03-05
 */
@Getter
@Setter
@Accessors(chain = true)
public class LogOrderGoods extends Model<LogOrderGoods> {

    /**
     * 自增ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 订单ID
     */
    @TableField("order_id")
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
     * 购买数量
     */
    @TableField("count")
    private Integer count;

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
     * 操作
     */
    @TableField("operation")
    private String operation;


    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
