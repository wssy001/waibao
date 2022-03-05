package com.waibao.payment.entity;

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
@TableName("user_credit")
public class UserCredit extends Model<UserCredit> {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("`money`")
    private Long userId;

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