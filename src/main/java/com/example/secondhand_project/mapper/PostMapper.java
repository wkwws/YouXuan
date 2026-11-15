package com.example.secondhand_project.mapper; // Mapper 所在的包

import com.example.secondhand_project.pojo.dto.FeedCandidate; // 候选集对象
import com.example.secondhand_project.pojo.entity.Post; // 说说实体
import com.baomidou.mybatisplus.core.mapper.BaseMapper; // MyBatis-Plus 基础 CRUD
import org.apache.ibatis.annotations.Param; // 给 SQL 参数起名
import org.apache.ibatis.annotations.Select; // 直接写 SQL

import java.time.LocalDateTime; // 时间参数
import java.util.List; // 返回多条候选

/**
 * <p>
 * 说说表（校园生活圈） Mapper 接口
 * </p>
 *
 * @author wkw
 * @since 2026-05-31
 */
public interface PostMapper extends BaseMapper<Post> { // 继承基础 Mapper，自带增删改查

    /**
     * 随机池候选：只取权重需要的字段，避免读正文/图片。
     */
    @Select("SELECT id, like_count AS likeCount, created_at AS createdAt " + // 只查三个字段：ID、点赞数、发布时间
            "FROM post WHERE created_at >= #{since} " + // 只拿最近 N 天的帖子
            "AND visible = 1 AND audit_status not in (0,2) " + // 隐藏/驳回的不进随机池
            "ORDER BY created_at DESC LIMIT #{limit}") // 按时间倒序，最多拿 limit 条
    List<FeedCandidate> selectFeedCandidates(@Param("since") LocalDateTime since, // 时间下限
                                             @Param("limit") int limit); // 最多查多少条

    /**
     * 全量最新候选：小站最近 N 天不足一页时兜底，避免 Feed 为空。
     */
    @Select("SELECT id, like_count AS likeCount, created_at AS createdAt " + // 同样只查三个字段
            "FROM post WHERE visible = 1 AND audit_status not in (0,2) " + // 隐藏/驳回的不进随机池
            "ORDER BY created_at DESC LIMIT #{limit}") // 不限制时间，直接拿最新 limit 条
    List<FeedCandidate> selectRecentCandidates(@Param("limit") int limit); // 最多查多少条
}
