-- ============================================================
-- 点赞功能：说说表只需要增加一个点赞总数字段
-- 执行前请确认数据库：secondhand_market
-- ============================================================

ALTER TABLE `post`
  ADD COLUMN `like_count` INT UNSIGNED NOT NULL DEFAULT 0
    COMMENT '点赞总数' AFTER `imgs`;
