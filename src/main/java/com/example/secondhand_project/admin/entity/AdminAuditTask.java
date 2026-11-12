package com.example.secondhand_project.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 待审核 UGC 队列。
 *
 * <p>商品/说说发布后写入一条记录，管理员逐条审核。内容本身仍保存在
 * product/post 表，本表只保存审核状态和提交时的快照，便于审核页展示。</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("admin_audit_task")
public class AdminAuditTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** PRODUCT / POST */
    private String targetType;

    private Integer targetId;

    private Integer submitterId;

    private String title;

    private String content;

    /** 提交时的完整快照 JSON，避免内容后续变化影响审核展示 */
    private String snapshotJson;

    /** 0-待审核 1-通过 2-驳回 */
    private Integer status;

    private String reason;

    private Integer reviewerId;

    private LocalDateTime reviewedAt;

    private LocalDateTime createdAt;
}
