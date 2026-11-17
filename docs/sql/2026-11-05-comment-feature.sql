-- ============================================================
-- 说说评论 / 评论点赞功能：补齐实体类用到的表和字段
-- 前置：先执行 2026-11-04-like-count.sql（post.like_count）
-- 执行前请确认数据库：secondhand_market
-- ============================================================

-- 1. 说说表增加评论总数（Post 实体已映射 comment_count，
--    缺这一列会导致 /posts 查询、发布说说、删除说说全部报 Unknown column）
ALTER TABLE `post`
  ADD COLUMN `comment_count` INT UNSIGNED NOT NULL DEFAULT 0
    COMMENT '评论总数（含子评论）' AFTER `like_count`;

-- 2. 一级评论表（Comment 实体）
CREATE TABLE IF NOT EXISTS `comment` (
  `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `post_id`     INT UNSIGNED    NOT NULL                 COMMENT '所属说说ID，关联 post.id',
  `uid`         INT UNSIGNED    NOT NULL                 COMMENT '评论用户ID，关联 user.id',
  `content`     VARCHAR(500)    NOT NULL                 COMMENT '评论内容，最长500字',
  `like_count`  INT UNSIGNED    NOT NULL DEFAULT 0       COMMENT '评论点赞数',
  `reply_count` INT UNSIGNED    NOT NULL DEFAULT 0       COMMENT '子评论数',
  `status`      TINYINT         NOT NULL DEFAULT 1       COMMENT '1-正常 0-已删除',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP
                                 ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_status_created` (`post_id`, `status`, `created_at`),
  KEY `idx_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='说说一级评论表';

-- 3. 子评论表（SubComment 实体）
CREATE TABLE IF NOT EXISTS `sub_comment` (
  `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '子评论ID',
  `comment_id`   BIGINT UNSIGNED NOT NULL                 COMMENT '所属一级评论ID',
  `reply_sub_id` BIGINT UNSIGNED NULL DEFAULT NULL        COMMENT '回复的子评论ID；NULL=直接回复一级评论',
  `uid`          INT UNSIGNED    NOT NULL                 COMMENT '回复者用户ID',
  `reply_uid`    INT UNSIGNED    NULL DEFAULT NULL        COMMENT '被回复用户ID',
  `content`      VARCHAR(500)    NOT NULL                 COMMENT '评论内容，最长500字',
  `status`       TINYINT         NOT NULL DEFAULT 1       COMMENT '1-正常 0-已删除',
  `created_at`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '回复时间',
  PRIMARY KEY (`id`),
  KEY `idx_comment_status_created` (`comment_id`, `status`, `created_at`),
  KEY `idx_reply_sub` (`reply_sub_id`),
  KEY `idx_uid` (`uid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='说说子评论表';

-- 4. 统一点赞记录表（LikedRecord 实体已映射为 like_record）
CREATE TABLE IF NOT EXISTS `like_record` (
  `id`          BIGINT UNSIGNED  NOT NULL AUTO_INCREMENT COMMENT '点赞记录ID',
  `target_type` TINYINT UNSIGNED NOT NULL                 COMMENT '1-说说 2-评论',
  `target_id`   BIGINT UNSIGNED  NOT NULL                 COMMENT '目标ID',
  `uid`         INT UNSIGNED     NOT NULL                 COMMENT '点赞用户ID',
  `created_at`  DATETIME         NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_target_uid` (`target_type`, `target_id`, `uid`),
  KEY `idx_uid_created` (`uid`, `created_at`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='统一点赞记录表';
