-- ============================================================
-- 校园二手交易平台 · 数据库初始化脚本 (v2.0 + WebSocket)
-- 数据库: MySQL 8.0+
-- 字符集: utf8mb4（支持 emoji）
-- ============================================================

CREATE DATABASE IF NOT EXISTS `secondhand_market`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE `secondhand_market`;

-- ============================================================
-- 1. 用户表 user
-- 字段明晰：username(用户名) / nickname(昵称) / phone(手机)
--           email(邮箱) / pwd(密码) / avatar(头像) / bio(简介)
--           last_online(最后在线时间) — WebSocket 用
-- ============================================================
CREATE TABLE IF NOT EXISTS `user` (
  `id`          INT UNSIGNED    NOT NULL AUTO_INCREMENT  COMMENT '用户ID，主键',
  `username`    VARCHAR(32)     NOT NULL                 COMMENT '用户名，登录用，唯一',
  `nickname`    VARCHAR(32)     NOT NULL DEFAULT ''       COMMENT '昵称，展示用',
  `email`       VARCHAR(128)    NOT NULL DEFAULT ''       COMMENT '邮箱',
  `phone`       VARCHAR(20)     NOT NULL DEFAULT ''       COMMENT '手机号',
  `pwd`         VARCHAR(255)    NOT NULL                 COMMENT '密码（bcrypt 哈希）',
  `avatar`      VARCHAR(512)    NOT NULL DEFAULT ''       COMMENT '头像 URL',
  `bio`         VARCHAR(200)    NOT NULL DEFAULT ''       COMMENT '个人简介',
  `last_online` DATETIME                                COMMENT '最后在线时间，WebSocket 断连时更新',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '注册时间',
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================================
-- 2. 分类表 category
-- 字段明晰：name(分类名称) / val(英文标识) / icon(图标)
--           sort_order(排序)
-- ============================================================
CREATE TABLE IF NOT EXISTS `category` (
  `id`          INT UNSIGNED    NOT NULL AUTO_INCREMENT  COMMENT '分类ID，主键',
  `name`        VARCHAR(20)     NOT NULL                 COMMENT '分类名称，如"电子产品"',
  `val`         VARCHAR(30)     NOT NULL                 COMMENT '分类标识，如 electronics',
  `icon`        VARCHAR(10)     NOT NULL DEFAULT ''       COMMENT '分类图标 emoji',
  `sort_order`  INT             NOT NULL DEFAULT 0       COMMENT '排序权重，越小越靠前',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_val` (`val`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- 预置分类数据
INSERT INTO `category` (`name`, `val`, `icon`, `sort_order`) VALUES
  ('家具',     'furniture',   '🪑', 1),
  ('电子产品', 'electronics', '📱', 2),
  ('工艺',     'crafts',      '🎨', 3),
  ('服装',     'clothing',    '👕', 4),
  ('数字产品', 'digital',     '💻', 5),
  ('运动',     'sports',      '⚽', 6),
  ('其他',     'other',       '📦', 7);

-- ============================================================
-- 3. 商品表 product
-- 字段明晰：title(标题) / desc(描述) / price(价格)
--           category_id(分类) / uid(发布者)
--           imgs(图片JSON) / status(状态)
-- ============================================================
CREATE TABLE IF NOT EXISTS `product` (
  `id`          INT UNSIGNED    NOT NULL AUTO_INCREMENT  COMMENT '商品ID，主键',
  `title`       VARCHAR(50)     NOT NULL                 COMMENT '商品标题',
  `desc`        VARCHAR(500)    NOT NULL DEFAULT ''       COMMENT '商品描述',
  `price`       DECIMAL(10,2)   NOT NULL DEFAULT 0.00    COMMENT '价格（元）',
  `category_id` INT UNSIGNED    NOT NULL DEFAULT 0       COMMENT '分类ID，关联 category.id',
  `uid`         INT UNSIGNED    NOT NULL DEFAULT 0       COMMENT '发布者用户ID，关联 user.id',
  `imgs`        JSON                                    COMMENT '图片URL数组，如 ["url1","url2"]',
  `status`      TINYINT         NOT NULL DEFAULT 0       COMMENT '商品状态：0-在售 1-已售出 2-下架',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '发布时间',
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_uid` (`uid`),
  KEY `idx_category` (`category_id`),
  KEY `idx_status` (`status`),
  KEY `idx_created` (`created_at`),
  FULLTEXT KEY `ft_search` (`title`, `desc`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- ============================================================
-- 4. 会话表 conversation
-- 字段明晰：uid1(参与方A) / uid2(参与方B)
--           last_msg(最后消息) / last_msg_type(最后消息类型)
--           last_time(最后时间)
--           unread_uid1(A的未读) / unread_uid2(B的未读)
-- ============================================================
CREATE TABLE IF NOT EXISTS `conversation` (
  `id`            INT UNSIGNED    NOT NULL AUTO_INCREMENT  COMMENT '会话ID，主键',
  `uid1`          INT UNSIGNED    NOT NULL                 COMMENT '参与方A，关联 user.id',
  `uid2`          INT UNSIGNED    NOT NULL                 COMMENT '参与方B，关联 user.id',
  `last_msg`      VARCHAR(500)    NOT NULL DEFAULT ''       COMMENT '最后一条消息的摘要',
  `last_msg_type` VARCHAR(10)     NOT NULL DEFAULT 'text'   COMMENT '最后消息类型: text/image/system',
  `last_time`     DATETIME                                COMMENT '最后一条消息的时间',
  `unread_uid1`   INT             NOT NULL DEFAULT 0       COMMENT 'uid1 的未读数',
  `unread_uid2`   INT             NOT NULL DEFAULT 0       COMMENT 'uid2 的未读数',
  `created_at`    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_users` (`uid1`, `uid2`),
  KEY `idx_uid1` (`uid1`),
  KEY `idx_uid2` (`uid2`),
  KEY `idx_last_time` (`last_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信会话表';

-- ============================================================
-- 5. 消息表 message
-- 字段明晰：conv_id(会话ID) / from_uid(发送方) / to_uid(接收方)
--           content(文字) / image(图片)
--           msg_type(消息类型) / status(送达状态)
-- 状态机：sent(已发送) → delivered(已送达) → read(已读)
-- ============================================================
CREATE TABLE IF NOT EXISTS `message` (
  `id`          INT UNSIGNED    NOT NULL AUTO_INCREMENT  COMMENT '消息ID，主键',
  `conv_id`     INT UNSIGNED    NOT NULL                 COMMENT '所属会话ID，关联 conversation.id',
  `from_uid`    INT UNSIGNED    NOT NULL                 COMMENT '发送方用户ID',
  `to_uid`      INT UNSIGNED    NOT NULL                 COMMENT '接收方用户ID',
  `content`     VARCHAR(1000)   NOT NULL DEFAULT ''       COMMENT '文字消息内容',
  `image`       VARCHAR(512)    NOT NULL DEFAULT ''       COMMENT '图片消息URL',
  `msg_type`    VARCHAR(10)     NOT NULL DEFAULT 'text'   COMMENT '消息类型: text(文字) / image(图片) / system(系统通知)',
  `status`      TINYINT         NOT NULL DEFAULT 0        COMMENT '送达状态: 0-sent(已发送) 1-delivered(已送达) 2-read(已读)',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '发送时间',
  PRIMARY KEY (`id`),
  KEY `idx_conv` (`conv_id`),
  KEY `idx_from` (`from_uid`),
  KEY `idx_to` (`to_uid`),
  KEY `idx_created` (`created_at`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息记录表';

-- ============================================================
-- 6. 说说表 post
-- 字段明晰：uid(发布者) / content(正文) / imgs(图片)
-- ============================================================
CREATE TABLE IF NOT EXISTS `post` (
  `id`          INT UNSIGNED    NOT NULL AUTO_INCREMENT  COMMENT '说说ID，主键',
  `uid`         INT UNSIGNED    NOT NULL                 COMMENT '发布者用户ID，关联 user.id',
  `content`     VARCHAR(500)    NOT NULL DEFAULT ''       COMMENT '文字内容',
  `imgs`        JSON                                     COMMENT '图片URL数组',
  `created_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '发布时间',
  `updated_at`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP  COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_uid` (`uid`),
  KEY `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='说说表（校园生活圈）';

-- ============================================================
-- 7. 上传文件表 upload
-- 字段明晰：filename(存储名) / origin_name(原始名)
--           url(访问路径) / mime(类型) / fsize(大小)
--           uid(上传者)
-- ============================================================
CREATE TABLE IF NOT EXISTS `upload` (
  `id`           INT UNSIGNED   NOT NULL AUTO_INCREMENT  COMMENT '文件ID，主键',
  `filename`     VARCHAR(128)   NOT NULL                 COMMENT '存储文件名（UUID 重命名）',
  `origin_name`  VARCHAR(256)   NOT NULL                 COMMENT '原始文件名',
  `url`          VARCHAR(512)   NOT NULL                 COMMENT '文件访问路径',
  `mime`         VARCHAR(50)    NOT NULL DEFAULT ''       COMMENT 'MIME 类型，如 image/jpeg',
  `fsize`        INT UNSIGNED   NOT NULL DEFAULT 0       COMMENT '文件大小（字节）',
  `uid`          INT UNSIGNED   NOT NULL DEFAULT 0       COMMENT '上传者用户ID',
  `created_at`   DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '上传时间',
  PRIMARY KEY (`id`),
  KEY `idx_uid` (`uid`),
  KEY `idx_mime` (`mime`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文件上传记录表';

-- ============================================================
-- 完成！
-- 表关系概览：
--   product.uid       → user.id
--   product.category_id → category.id
--   message.conv_id   → conversation.id
--   message.from_uid  → user.id
--   message.to_uid    → user.id
--   conversation.uid1 → user.id
--   conversation.uid2 → user.id
--   upload.uid        → user.id
--   post.uid          → user.id
--
-- WebSocket 加持后的消息流转：
--   用户A发送 → POST /api/messages → 入库(status=sent)
--           → WebSocket 推送 → 用户B在线则 status→delivered
--           → 用户B已读 → POST /api/messages/read → status→read
-- ============================================================
