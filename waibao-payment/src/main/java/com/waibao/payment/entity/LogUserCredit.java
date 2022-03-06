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
 * @since 2022-03-06
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("log_user_credit")
public class LogUserCredit extends Model<LogUserCredit> {

    /**
     * 自增id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 交易id
     */
    @TableField("pay_id")
    private Long payId;

    /**
     * 订单ID
     */
    @TableField("order_id")
    private String orderId;

    /**
     * 先前的余额
     */
    @TableField("old_money")
    private BigDecimal oldMoney;

    /**
     * 余额
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
