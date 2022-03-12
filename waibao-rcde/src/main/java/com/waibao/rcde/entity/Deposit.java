package com.waibao.rcde.entity;

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
 * @since 2022-03-10
 */
@Getter
@Setter
@Accessors(chain = true)
public class Deposit extends Model<Deposit> {

    /**
     * 自增ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 还款人ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 欠债金额
     */
    @TableField("debt_amount")
    private BigDecimal debtAmount;

    /**
     * 还款日
     */
    @TableField("due_date")
    private Date dueDate;

    /**
     * 还款金额
     */
    @TableField("deposit_amount")
    private BigDecimal depositAmount;

    /**
     * 还款日
     */
    @TableField("deposit_date")
    private Date depositDate;

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


    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
