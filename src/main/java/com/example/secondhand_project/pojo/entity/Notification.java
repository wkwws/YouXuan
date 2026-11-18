package com.example.secondhand_project.pojo.entity; // 声明当前类所在的实体包

import com.baomidou.mybatisplus.annotation.IdType; // MyBatis-Plus 主键生成策略枚举
import com.baomidou.mybatisplus.annotation.TableField; // 用于把 Java 字段映射到数据库列
import com.baomidou.mybatisplus.annotation.TableId; // 用于标记主键字段
import com.baomidou.mybatisplus.annotation.TableName; // 用于指定实体对应的数据库表名
import lombok.Data; // Lombok 自动生成 getter/setter/toString 等
import lombok.EqualsAndHashCode; // Lombok 自动生成 equals/hashCode
import lombok.experimental.Accessors; // Lombok 支持链式 setter
import java.io.Serializable; // 标记该对象可序列化
import java.time.LocalDateTime; // 表示通知创建时间

/**
 * 互动通知：评论、回复等事件给用户的消息。
 */
@Data // 自动生成 getter/setter，供 MyBatis 和 JSON 序列化使用
@EqualsAndHashCode(callSuper = false) // 只按当前类字段比较，不比较父类
@Accessors(chain = true) // setter 返回当前对象，便于链式赋值
@TableName("notification") // 对应数据库 notification 表
public class Notification implements Serializable { // 实体类实现序列化接口

    private static final long serialVersionUID = 1L; // 序列化版本号，避免对象结构变化时反序列化失败

    @TableId(value = "id", type = IdType.AUTO) // 指定主键列 id，并由数据库自增生成
    private Long id; // 通知主键 ID

    /** 接收人 user.id */
    @TableField("recipient_uid") // Java 字段名 recipientUid 映射到数据库列 recipient_uid
    private Integer recipientUid; // 这条通知由哪个用户接收

    /** 触发通知的用户 user.id */
    @TableField("actor_uid") // Java 字段名 actorUid 映射到数据库列 actor_uid
    private Integer actorUid; // 谁评论或回复触发了这条通知

    /** POST_COMMENT / POST_REPLY / COMMENT_REPLY */
    @TableField("type") // 通知类型映射到数据库 type 列
    private String type; // 通知类型：评论说说 / 回复说说 / 回复评论

    @TableField("post_id") // 关联的说说 ID 列
    private Integer postId; // 被评论或回复的说说 ID

    @TableField("comment_id") // 关联的一级评论 ID 列
    private Long commentId; // 这条通知对应的一级评论 ID

    @TableField("sub_comment_id") // 关联的子评论 ID 列
    private Long subCommentId; // 如果是子评论回复则记录子评论 ID，否则为 null

    /** 触发通知的评论/回复内容 */
    @TableField("content") // 触发通知的评论内容列
    private String content; // 评论或回复正文，通知列表直接展示

    /** 说说内容摘要，用于通知列表展示 */
    @TableField("post_excerpt") // 说说摘要列
    private String postExcerpt; // 说说正文摘要，帮助用户理解通知语境

    /** 0-未读 1-已读 */
    @TableField("is_read") // 已读状态列
    private Integer isRead; // 0 表示未读，1 表示已读

    @TableField("created_at") // 通知创建时间列
    private LocalDateTime createdAt; // 通知产生时间，用于列表倒序展示
}
