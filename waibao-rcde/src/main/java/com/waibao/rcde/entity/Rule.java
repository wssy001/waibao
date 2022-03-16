package com.waibao.rcde.entity;

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
 * @since 2022-03-11
 */
@Getter
@Setter
@Accessors(chain = true)
public class Rule extends Model<Rule> {

    /**
     * 自增ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 秒杀商品id
     */
    @TableField("goods_id")
    private Long goodsId;

    /**
     * 规则码
     */
    @TableField("rule_code")
    private Integer ruleCode;

    /**
     * 允许延迟几天还款
     */
    @TableField("allow_overdue_delayed_days")
    private Integer allowOverdueDelayedDays;

    /**
     * 拒绝失信人
     */
    @TableField("deny_defaulter")
    private Boolean denyDefaulter;

    /**
     * 拒绝拥有几条逾期记录
     */
    @TableField("deny_overdue_times")
    private Integer denyOverdueTimes;

    /**
     * 逾期记录统计年限
     */
    @TableField("collect_years")
    private Integer collectYears;

    /**
     * 忽略欠款多少的逾期记录
     */
    @TableField("ignore_overdue_amount")
    private BigDecimal ignoreOverdueAmount;

    /**
     * 拒绝低于这个年龄的
     */
    @TableField("deny_age_below")
    private Integer denyAgeBelow;

    /**
     * 用户类型
     */
    @TableField("user_type")
    private Integer userType;

    @TableField("deny_work_status")
    private String denyWorkStatus;

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
