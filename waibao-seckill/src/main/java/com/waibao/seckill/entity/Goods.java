package com.waibao.seckill.entity;

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
 * @since 2022-03-30
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("goods")
public class Goods extends Model<Goods> {

    /**
     * 自增ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品名
     */
    @TableField("`name`")
    private String name;

    /**
     * 描述
     */
    @TableField("description")
    private String description;

    /**
     * 生效时间
     */
    @TableField("start_time")
    private Date startTime;

    /**
     * 失效时间
     */
    @TableField("end_time")
    private Date endTime;

    /**
     * 金额
     */
    @TableField("price")
    private BigDecimal price;

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
