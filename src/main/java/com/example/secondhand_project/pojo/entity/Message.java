package com.example.secondhand_project.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 消息记录表
 * </p>
 *
 * @author wkw
 * @since 2026-06-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("message")
public class Message implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息ID，主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 所属会话ID，关联 conversation.id
     */
    @TableField("conv_id")
    private Integer conversationId;

    /**
     * 发送方用户ID
     */
    @TableField("from_uid")
    private Integer fromUserId;

    /**
     * 接收方用户ID
     */
    @TableField("to_uid")
    private Integer toUserId;

    /**
     * 文字消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 图片消息URL
     */
    @TableField("image")
    private String image;

    /**
     * 消息类型: text(文字) / image(图片) / system(系统通知)
     */
    @TableField("msg_type")
    private String msgType;

    /**
     * 送达状态: 0-sent(已发送) 1-delivered(已送达) 2-read(已读)
     */
    @TableField("status")
    private Integer status;

    /**
     * 发送时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;


}
