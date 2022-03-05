package com.waibao.payment.entitiy;


import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
@Accessors(chain = true)
@TableName("payment")
public class Payment extends Model<Payment> {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("`pay_id`")
    private Long payId;

    @TableField("`user_id`")
    private Long userId;

    @TableField("`order_id`")
    private Long orderId;

    @TableField("`goods_id`")
    private Long goodsId;

    @TableField("`money`")
    private Long money;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private Date createTime;

    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @Override
    public Serializable pkVal() {
        return this.id;
    }
}