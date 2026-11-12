package com.example.secondhand_project.pojo.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 用户表
 * </p>
 *
 * @author wkw
 * @since 2026-05-27
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("user")
@Schema(description = "用户实体")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户ID，主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "用户ID")
    private Integer id;

    /**
     * 用户名，登录用，唯一
     */
    @Schema(description = "用户名（登录用）", example = "zhangsan")
    private String username;

    /**
     * 昵称，展示用
     */
    @Schema(description = "昵称（展示用）", example = "张三")
    private String nickname;

    /**
     * 邮箱
     */
    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    /**
     * 手机号
     */
    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    /**
     * 密码（bcrypt 哈希）
     */
    @JsonIgnore
    @Schema(description = "密码（bcrypt 哈希，响应中不返回）")
    private String pwd;

    /**
     * 头像 URL
     */
    @Schema(description = "头像URL")
    private String avatar;

    /**
     * 个人简介
     */
    @Schema(description = "个人简介")
    private String bio;

    /**
     * 角色：0-普通用户 1-管理员 2-超级管理员
     */
    @Schema(description = "角色：0-普通用户 1-管理员 2-超级管理员")
    private Integer role;

    /**
     * 状态：0-正常 1-封禁
     */
    @Schema(description = "状态：0-正常 1-封禁")
    private Integer status;

    /**
     * 封禁原因
     */
    @Schema(description = "封禁原因")
    private String banReason;

    /**
     * 封禁时间
     */
    @Schema(description = "封禁时间")
    private LocalDateTime bannedAt;

    /**
     * 解封时间，NULL 表示永久
     */
    @Schema(description = "解封时间，NULL 表示永久")
    private LocalDateTime bannedUntil;

    /**
     * 最后在线时间，WebSocket 断连时更新
     */
    @Schema(description = "最后在线时间")
    private LocalDateTime lastOnline;

    /**
     * 注册时间
     */
    @Schema(description = "注册时间")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;


}
