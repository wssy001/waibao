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
 * 用户基本信息表
 * </p>
 *
 * @author alexpetertyler
 * @since 2022-02-15
 */
@Getter
@Setter
@Accessors(chain = true)
public class User extends Model<User> {

    /**
     * 自增ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 手机号
     */
    @TableField("mobile")
    private String mobile;

    /**
     * 邮箱地址
     */
    @TableField("email")
    private String email;

    /**
     * 密码
     */
    @TableField("`password`")
    private String password;

    /**
     * 性别
     */
    @TableField("sex")
    private Integer sex;

    /**
     * 用户昵称
     */
    @TableField("nickname")
    private String nickname;

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
