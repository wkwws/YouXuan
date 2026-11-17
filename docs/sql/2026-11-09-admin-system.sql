-- 管理后台基础版：角色、封禁、UGC 审核、操作日志
-- 适用库：secondhand_market（MySQL 8）
-- 本脚本是幂等的：重复执行不会因为列/索引已存在而报错。
-- 执行前仍建议先备份。

-- ============================================================
-- user：角色、封禁
-- ============================================================
SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'role');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `user` ADD COLUMN `role` TINYINT NOT NULL DEFAULT 0 COMMENT ''0-普通用户 1-管理员 2-超级管理员''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'status');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `user` ADD COLUMN `status` TINYINT NOT NULL DEFAULT 0 COMMENT ''0-正常 1-封禁''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'ban_reason');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `user` ADD COLUMN `ban_reason` VARCHAR(200) NOT NULL DEFAULT '''' COMMENT ''封禁原因''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'banned_at');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `user` ADD COLUMN `banned_at` DATETIME NULL COMMENT ''封禁时间''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND COLUMN_NAME = 'banned_until');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `user` ADD COLUMN `banned_until` DATETIME NULL COMMENT ''解封时间，NULL 表示永久''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'user' AND INDEX_NAME = 'idx_role_status');
SET @ddl := IF(@has_idx = 0,
  'ALTER TABLE `user` ADD INDEX `idx_role_status` (`role`, `status`)',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- product：审核状态、下架原因
-- ============================================================
SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'audit_status');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `product` ADD COLUMN `audit_status` TINYINT NOT NULL DEFAULT 1 COMMENT ''0-待审核 1-通过 2-驳回/封禁''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'audit_remark');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `product` ADD COLUMN `audit_remark` VARCHAR(200) NOT NULL DEFAULT '''' COMMENT ''审核备注/驳回原因''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'audited_by');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `product` ADD COLUMN `audited_by` INT NULL COMMENT ''审核人 user.id''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'audited_at');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `product` ADD COLUMN `audited_at` DATETIME NULL COMMENT ''审核时间''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND COLUMN_NAME = 'offline_reason');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `product` ADD COLUMN `offline_reason` VARCHAR(200) NOT NULL DEFAULT '''' COMMENT ''管理员下架/封禁原因''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'product' AND INDEX_NAME = 'idx_audit_created');
SET @ddl := IF(@has_idx = 0,
  'ALTER TABLE `product` ADD INDEX `idx_audit_created` (`audit_status`, `created_at`)',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- post：审核状态、可见性、隐藏原因
-- ============================================================
SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND COLUMN_NAME = 'audit_status');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `post` ADD COLUMN `audit_status` TINYINT NOT NULL DEFAULT 1 COMMENT ''0-待审核 1-通过 2-驳回/封禁''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND COLUMN_NAME = 'visible');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `post` ADD COLUMN `visible` TINYINT NOT NULL DEFAULT 1 COMMENT ''1-可见 0-隐藏''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND COLUMN_NAME = 'audit_remark');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `post` ADD COLUMN `audit_remark` VARCHAR(200) NOT NULL DEFAULT '''' COMMENT ''审核备注/驳回原因''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND COLUMN_NAME = 'hidden_reason');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `post` ADD COLUMN `hidden_reason` VARCHAR(200) NOT NULL DEFAULT '''' COMMENT ''管理员隐藏原因''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND COLUMN_NAME = 'audited_by');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `post` ADD COLUMN `audited_by` INT NULL COMMENT ''审核人 user.id''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_col := (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND COLUMN_NAME = 'audited_at');
SET @ddl := IF(@has_col = 0,
  'ALTER TABLE `post` ADD COLUMN `audited_at` DATETIME NULL COMMENT ''审核时间''',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @has_idx := (SELECT COUNT(*) FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'post' AND INDEX_NAME = 'idx_audit_visible_created');
SET @ddl := IF(@has_idx = 0,
  'ALTER TABLE `post` ADD INDEX `idx_audit_visible_created` (`audit_status`, `visible`, `created_at`)',
  'DO 0');
PREPARE stmt FROM @ddl; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ============================================================
-- 新表
-- ============================================================
CREATE TABLE IF NOT EXISTS `admin_audit_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `target_type` VARCHAR(20) NOT NULL COMMENT 'PRODUCT/POST',
  `target_id` INT NOT NULL,
  `submitter_id` INT NOT NULL,
  `title` VARCHAR(255) NOT NULL DEFAULT '',
  `content` TEXT NULL,
  `snapshot_json` JSON NULL,
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '0-待审核 1-通过 2-驳回',
  `reason` VARCHAR(200) NOT NULL DEFAULT '',
  `reviewer_id` INT NULL,
  `reviewed_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_target` (`target_type`, `target_id`),
  KEY `idx_status_created` (`status`, `created_at`),
  KEY `idx_submitter` (`submitter_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='待审核 UGC 队列';

CREATE TABLE IF NOT EXISTS `admin_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `admin_id` INT NOT NULL,
  `admin_name` VARCHAR(32) NOT NULL DEFAULT '',
  `module` VARCHAR(50) NOT NULL,
  `action` VARCHAR(50) NOT NULL,
  `target` VARCHAR(255) NOT NULL DEFAULT '',
  `result` VARCHAR(10) NOT NULL DEFAULT '成功',
  `ip` VARCHAR(64) NOT NULL DEFAULT '',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_created` (`created_at`),
  KEY `idx_module` (`module`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='后台操作日志';

-- 默认管理员：admin / admin123（MD5，与现有登录逻辑一致）
INSERT INTO `user` (`username`, `nickname`, `email`, `phone`, `pwd`, `avatar`, `bio`, `role`, `status`)
VALUES ('admin', '管理员', 'admin@liuzhuanpin.local', '', '0192023a7bbd73250516f069df18b500', '', '流转品管理后台', 2, 0)
ON DUPLICATE KEY UPDATE `role` = 2, `status` = 0, `nickname` = '管理员';
