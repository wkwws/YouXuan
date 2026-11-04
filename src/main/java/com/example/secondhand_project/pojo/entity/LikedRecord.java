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
 * 点赞记录表（对应模板 tj-remark 的 liked_record）
 * </p>
 *
 * <p>
 * 说明：模板当前启用的是 Redis 版本（LikedRedisRecordServiceImpl），
 * 点赞关系直接存在 Redis Set 中，这张表只在后续需要“数据库兜底/对账”时使用。
 * </p>
 *
 * @author wkw
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("liked_record")
public class LikedRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键id
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    @Schema(description = "点赞用户ID")
    private Long userId;

    /**
     * 点赞的业务id
     */
    @Schema(description = "点赞业务ID，例如说说ID")
    private Long bizId;

    /**
     * 点赞的业务类型
     */
    @Schema(description = "业务类型：POST=说说")
    private String bizType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

}
