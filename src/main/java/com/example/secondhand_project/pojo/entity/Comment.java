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
 * 说说一级评论表
 * </p>
 *
 * @author wkw
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("comment")
public class Comment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 评论ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属说说ID
     */
    @TableField("post_id")
    private Integer postId;

    /**
     * 评论用户ID
     */
    @TableField("uid")
    private Integer uid;

    /**
     * 评论内容，最长500字
     */
    @TableField("content")
    private String content;

    /**
     * 评论点赞数
     */
    @TableField("like_count")
    private Integer likeCount;

    /**
     * 子评论数
     */
    @TableField("reply_count")
    private Integer replyCount;

    /**
     * 1-正常 0-已删除
     */
    @TableField("status")
    private Integer status;

    /**
     * 评论时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

}
