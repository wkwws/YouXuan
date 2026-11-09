package com.example.secondhand_project.util; // 工具类所在的包

import com.example.secondhand_project.conf.FeedProperties; // 读取权重参数

import java.time.Duration; // 计算两个时间相差多久
import java.time.LocalDateTime; // 时间类型

/**
 * 说说随机权重计算。
 *
 * <pre>
 * weight = 1 + 4 * exp(-ageHours / 36) + 1.5 * ln(1 + likeCount)
 * 新帖 2 小时内额外 + 2
 * 最终封顶 20
 * </pre>
 *
 * <p>基础分 1 保证老帖/零赞帖也有曝光机会；点赞用对数，避免高赞帖完全垄断。</p>
 */
public final class FeedWeightCalculator { // final 表示这个类不能被继承

    private FeedWeightCalculator() { // 私有构造器，防止 new 这个工具类
    }

    /**
     * 算一条帖子的“抽奖券数量”。
     *
     * @param likeCount  点赞数
     * @param createdAt  发布时间
     * @param now        当前时间
     * @param properties 权重参数
     * @return 权重，越大越容易被抽到
     */
    public static double weight(Integer likeCount, // 这条帖子有多少赞
                                LocalDateTime createdAt, // 这条帖子什么时候发的
                                LocalDateTime now, // 现在是什么时间
                                FeedProperties properties) { // 配置里的权重参数
        LocalDateTime publishedAt = createdAt == null ? now : createdAt; // 没有发布时间就按“现在”算，避免空指针
        double ageHours = Math.max(0, Duration.between(publishedAt, now).toMinutes() / 60.0); // 发布到现在多少小时；未来时间夹到 0
        double halfLife = Math.max(0.1, properties.getHalfLifeHours()); // 半衰期至少 0.1 小时，防止除以 0
        double recency = Math.exp(-ageHours / halfLife); // 时间分：越新越接近 1，越老越接近 0

        int likes = likeCount == null ? 0 : Math.max(0, likeCount); // 点赞数空值/负数都按 0 处理
        double likePart = Math.log1p(likes); // 点赞分：ln(1+赞)，100 赞不会比 10 赞夸张 10 倍

        double weight = 1.0 // 基础分：保证每条已发布帖子至少有机会
                + properties.getRecencyWeight() * recency // 加上时间分
                + properties.getLikeWeight() * likePart; // 加上点赞分

        if (ageHours <= properties.getNewPostHours()) { // 如果发布还没超过“新帖时间”
            weight += properties.getNewPostBoost(); // 额外加新帖分，让刚发的内容更容易被看到
        }

        return Math.min(weight, Math.max(1.0, properties.getMaxWeight())); // 封顶，防止一条爆款霸屏
    }
}
