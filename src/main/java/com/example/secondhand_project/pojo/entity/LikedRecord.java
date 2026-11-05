package com.example.secondhand_project.pojo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 统一点赞记录表（对应设计文档的 like_record）
 * </p>
 *
 * <p>
 * 当前点赞快路径走 Redis（LikedRedisRecordServiceImpl），这张表暂不写入；
 * 保留实体/Mapper 是为了以后加数据库兜底或对账时可以直接使用。
 * </p>
 *
 * @author wkw
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("like_record")
public class LikedRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 目标类型：1-说说 2-评论
     */
    @Schema(description = "目标类型：1-说说 2-评论")
    private Integer targetType;

    /**
     * 目标id
     */
    @Schema(description = "目标ID")
    private Long targetId;

    /**
     * 点赞用户id
     */
    @Schema(description = "点赞用户ID")
    private Integer uid;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

}
