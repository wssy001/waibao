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
 * 管理员表
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-02-15
 */
@Getter
@Setter
@Accessors(chain = true)
@TableName("admin")
public class Admin extends Model<Admin> {

    /**
     * 自增ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 管理员名称
     */
    @TableField("`name`")
    private String name;

    /**
     * 密码
     */
    @TableField("`password`")
    private String password;

    /**
     * 管理员级别
     */
    @TableField("`level`")
    private Integer level;

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
