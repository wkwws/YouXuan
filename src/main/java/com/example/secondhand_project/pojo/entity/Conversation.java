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
 * 私信会话表
 * </p>
 *
 * @author wkw
 * @since 2026-06-02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("conversation")
public class Conversation implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话ID，主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 参与方A，关联 user.id
     */
    @TableField("uid1")
    private Integer uid1;

    /**
     * 参与方B，关联 user.id
     */
    @TableField("uid2")
    private Integer uid2;

    /**
     * 最后一条消息的摘要
     */
    @TableField("last_msg")
    private String lastMsg;

    /**
     * 最后消息类型: text/image/system
     */
    @TableField("last_msg_type")
    private String lastMsgType;

    /**
     * 最后一条消息的时间
     */
    @TableField("last_time")
    private LocalDateTime lastTime;

    /**
     * uid1 的未读数
     */
    @TableField("unread_uid1")
    private Integer unreadUid1;

    /**
     * uid2 的未读数
     */
    @TableField("unread_uid2")
    private Integer unreadUid2;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;


}
