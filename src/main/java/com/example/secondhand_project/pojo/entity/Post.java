package com.example.secondhand_project.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import java.util.List;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 说说表（校园生活圈）
 * </p>
 *
 * @author wkw
 * @since 2026-05-31
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "post",autoResultMap = true)
public class Post implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 说说ID，主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;

    /**
     * 发布者用户ID，关联 user.id
     */
    @TableField("uid")
    private Integer uid;

    /**
     * 文字内容
     */
    @TableField("content")
    private String content;

    /**
     * 图片URL数组
     */
    @TableField(value = "imgs",typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    /**
     * 点赞总数
     */
    @TableField("like_count")
    private Integer likeCount;

    /**
     * 发布时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;


}
