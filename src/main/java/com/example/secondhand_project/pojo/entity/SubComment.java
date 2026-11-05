package com.example.secondhand_project.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 说说子评论表
 * </p>
 *
 * @author wkw
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("sub_comment")
public class SubComment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 子评论ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属一级评论ID
     */
    @TableField("comment_id")
    private Long commentId;

    /**
     * 回复的子评论ID；NULL=直接回复一级评论
     */
    @TableField("reply_sub_id")
    private Long replySubId;

    /**
     * 回复者用户ID
     */
    @TableField("uid")
    private Integer uid;

    /**
     * 被回复用户ID
     */
    @TableField("reply_uid")
    private Integer replyUid;

    /**
     * 评论内容，最长500字
     */
    @TableField("content")
    private String content;

    /**
     * 1-正常 0-已删除
     */
    @TableField("status")
    private Integer status; //逻辑删除

    /**
     * 回复时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

}
