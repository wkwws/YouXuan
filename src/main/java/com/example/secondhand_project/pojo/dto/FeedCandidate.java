package com.example.secondhand_project.pojo.dto; // DTO 所在的包

import lombok.Data; // 自动生成 getter/setter

import java.time.LocalDateTime; // 发布时间类型

/**
 * 随机池候选：只取重建权重需要的字段，避免把正文/图片整行读出来。
 */
@Data // 自动生成 getter/setter
public class FeedCandidate {

    private Integer id; // 帖子 ID，最后写进随机页的就是它

    private Integer likeCount; // 点赞数，用来算“点赞分”

    private LocalDateTime createdAt; // 发布时间，用来算“时间分”
}
