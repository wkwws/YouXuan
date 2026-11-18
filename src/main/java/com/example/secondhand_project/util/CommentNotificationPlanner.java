package com.example.secondhand_project.util; // 声明工具类所在包

import com.example.secondhand_project.pojo.entity.Comment; // 一级评论实体
import com.example.secondhand_project.pojo.entity.Notification; // 通知实体
import com.example.secondhand_project.pojo.entity.Post; // 说说实体
import com.example.secondhand_project.pojo.entity.SubComment; // 子评论实体
import java.time.LocalDateTime; // 通知创建时间
import java.util.ArrayList; // 存放本次需要生成的通知
import java.util.List; // 返回通知列表
import java.util.Objects; // 安全比较两个用户 ID

/**
 * 决定一次评论/回复应该通知谁。
 *
 * <p>规则：评论通知说说作者；回复通知被回复的人，同时通知说说作者。
 * 自己操作自己的内容不通知；同一个人只收到一条通知。</p>
 */
public final class CommentNotificationPlanner { // 纯规则类，不访问数据库

    public static final String POST_COMMENT = "POST_COMMENT"; // 别人评论我的说说

    public static final String POST_REPLY = "POST_REPLY"; // 别人在我发布的说说下回复

    public static final String COMMENT_REPLY = "COMMENT_REPLY"; // 别人回复我的评论或楼中楼

    private static final int EXCERPT_LENGTH = 120; // 说说摘要最大保留 120 个字符

    private CommentNotificationPlanner() { // 私有构造器防止实例化
    }

    public static List<Notification> forNewComment(Post post, Comment comment) { // 一级评论的通知计划
        List<Notification> result = new ArrayList<>(); // 保存本次需要插入的通知
        if (!canNotify(post, comment) || sameUser(post.getUid(), comment.getUid())) { // 数据不完整或自己评论自己时不通知
            return result; // 返回空列表
        }
        result.add(base(post, post.getUid(), comment.getUid(), POST_COMMENT, comment.getId(), null, // 接收人是说说作者
                comment.getContent())); // 通知内容就是一级评论正文
        return result; // 返回生成的通知
    }

    public static List<Notification> forNewReply(Post post, Comment comment, SubComment reply) { // 子评论/回复的通知计划
        List<Notification> result = new ArrayList<>(); // 保存本次需要插入的通知
        if (post == null || comment == null || reply == null) { // 任一关联对象为空时不生成通知
            return result; // 返回空列表
        }

        Integer actorUid = reply.getUid(); // 回复者就是触发通知的人
        if (actorUid == null) { // 匿名/脏数据没有回复者 ID
            return result; // 无法判断接收人，直接跳过
        }
        Integer replyToUid = reply.getReplyUid() != null ? reply.getReplyUid() : comment.getUid(); // 优先通知被回复的子评论作者，否则通知一级评论作者

        if (replyToUid != null && !sameUser(replyToUid, actorUid)) { // 被回复的人存在且不是自己时生成回复通知
            result.add(base(post, replyToUid, actorUid, COMMENT_REPLY, comment.getId(), // 接收人是被回复者
                    reply.getId(), reply.getContent())); // 记录子评论 ID 和回复正文
        }

        if (post.getUid() != null // 说说作者存在
                && !sameUser(post.getUid(), actorUid) // 说说作者不是回复者本人
                && !sameUser(post.getUid(), replyToUid)) { // 说说作者和被回复者不是同一人，避免重复通知
            result.add(base(post, post.getUid(), actorUid, POST_REPLY, comment.getId(), // 接收人是说说作者
                    reply.getId(), reply.getContent())); // 记录子评论 ID 和回复正文
        }

        return result; // 返回全部通知计划
    }

    private static Notification base(Post post, Integer recipientUid, Integer actorUid, String type, // 统一组装通知公共字段
                                     Long commentId, Long subCommentId, String content) { // 评论/子评论 ID 和正文
        return new Notification() // 创建通知实体
                .setRecipientUid(recipientUid) // 设置通知接收人
                .setActorUid(actorUid) // 设置通知触发人
                .setType(type) // 设置通知类型
                .setPostId(post.getId()) // 设置关联说说 ID
                .setCommentId(commentId) // 设置关联一级评论 ID
                .setSubCommentId(subCommentId) // 设置关联子评论 ID（可为空）
                .setContent(content) // 保存评论/回复正文
                .setPostExcerpt(excerpt(post.getContent())) // 保存说说摘要
                .setIsRead(0) // 新通知默认未读
                .setCreatedAt(LocalDateTime.now()); // 记录通知生成时间
    }

    private static boolean canNotify(Post post, Comment comment) { // 判断基础数据是否足以生成通知
        return post != null && comment != null && post.getUid() != null && comment.getUid() != null; // 说说、评论及双方作者都不能为空
    }

    private static boolean sameUser(Integer left, Integer right) { // 判断两个用户 ID 是否为同一人
        return left != null && Objects.equals(left, right); // 左边为空时视为无法比较，返回 false
    }

    private static String excerpt(String content) { // 生成说说摘要
        String text = content == null ? "" : content.trim(); // 空内容转为空字符串并去掉首尾空白
        return text.length() <= EXCERPT_LENGTH ? text : text.substring(0, EXCERPT_LENGTH) + "…"; // 超长时截断并加省略号
    }
}
