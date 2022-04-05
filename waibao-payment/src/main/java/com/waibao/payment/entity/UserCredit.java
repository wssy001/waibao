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
 * 账户信息表
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-04-05
 */
@Getter
@Setter
@Accessors(chain = true)
public class UserCredit extends Model<UserCredit> {

    @TableId(value = "user_id")
    private Long userId;

    @TableField("money")
    private BigDecimal money;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @Override
    public Serializable pkVal() {
        return this.userId;
    }

}
