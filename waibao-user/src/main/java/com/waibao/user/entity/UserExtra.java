package com.waibao.user.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

/**
 * <p>
 * 
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-03-14
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("user_extra")
public class UserExtra extends Model<UserExtra> {

    /**
     * 自增ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 是否为失信人
     */
    @TableField("defaulter")
    private Boolean defaulter;

    /**
     * 年龄
     */
    @TableField("age")
    private Integer age;

    /**
     * 工作状态
     */
    @TableField("work_status")
    private String workStatus;

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
