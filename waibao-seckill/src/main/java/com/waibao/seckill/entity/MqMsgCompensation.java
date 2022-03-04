package com.waibao.seckill.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
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
 * @since 2022-03-03
 */
@Getter
@Setter
@Accessors(chain = true)
public class MqMsgCompensation extends Model<MqMsgCompensation> {

    /**
     * 消息id
     */
    @TableId("msg_id")
    private String msgId;

    /**
     * topic
     */
    @TableField("topic")
    private String topic;

    /**
     * tags
     */
    @TableField("tags")
    private String tags;

    /**
     * 消息状态
     */
    @TableField("`status`")
    private String status;

    /**
     * 业务key
     */
    @TableField("business_key")
    private String businessKey;

    /**
     * 消息内容（JSON）
     */
    @TableField("content")
    private String content;

    /**
     * 异常信息
     */
    @TableField("exception_msg")
    private String exceptionMsg;

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
        return this.msgId;
    }

}
