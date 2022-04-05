package com.waibao.payment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
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
 * @since 2022-04-05
 */
@Getter
@Setter
@Accessors(chain = true)
public class LogUserCredit extends Model<LogUserCredit> {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("pay_id")
    private String payId;

    @TableField("order_id")
    private String orderId;

    @TableField("old_money")
    private BigDecimal oldMoney;

    @TableField("money")
    private BigDecimal money;

    @TableField("operation")
    private String operation;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;


    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
