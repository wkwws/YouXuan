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
 * 后台操作日志。只记录关键写操作，便于追溯。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("admin_operation_log")
public class AdminOperationLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Integer adminId;

    private String adminName;

    private String module;

    private String action;

    private String target;

    private String result;

    private String ip;

    private LocalDateTime createdAt;
}
