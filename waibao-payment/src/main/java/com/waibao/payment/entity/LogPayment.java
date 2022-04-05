package com.waibao.payment.entity;

import com.baomidou.mybatisplus.annotation.*;
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
 * @since 2022-04-05
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("log_payment")
public class LogPayment extends Model<LogPayment> {

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 支付id
     */
    @TableField("pay_id")
    private String payId;

    /**
     * 用户编号
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 订单id
     */
    @TableField("order_id")
    private String orderId;

    /**
     * 商品id
     */
    @TableField("goods_id")
    private Long goodsId;

    /**
     * 支付金额
     */
    @TableField("money")
    private BigDecimal money;

    /**
     * 操作类型
     */
    @TableField("operation")
    private String operation;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;


    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
