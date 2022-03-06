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
 * @since 2022-02-18
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("seckill_goods")
public class SeckillGoods extends Model<SeckillGoods> {

    /**
     * 自增ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 商品ID
     */
    @TableField("goods_id")
    private Long goodsId;

    /**
     * 卖家ID
     */
    @TableField("retailer_id")
    private Long retailerId;

    /**
     * 原价
     */
    @TableField("price")
    private BigDecimal price;

    /**
     * 秒杀价
     */
    @TableField("seckill_price")
    private BigDecimal seckillPrice;

    /**
     * 库存量
     */
    @TableField("`storage`")
    private Integer storage;

    /**
     * 每位顾客可购买量
     */
    @TableField("purchase_limit")
    private Integer purchaseLimit;

    /**
     * 秒杀开始时间
     */
    @TableField("seckill_start_time")
    private Date seckillStartTime;

    /**
     * 秒杀结束时间
     */
    @TableField("seckill_end_time")
    private Date seckillEndTime;


    @Override
    public Serializable pkVal() {
        return this.id;
    }

}
