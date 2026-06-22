-- ============================================================
-- V3:站内信通知 + 缺陷自定义字段定义(MySQL)
-- ============================================================

CREATE TABLE dt_notification (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '接收人',
  type        VARCHAR(16)  NOT NULL COMMENT 'ASSIGN/MENTION',
  content     VARCHAR(512) NOT NULL,
  ref_type    VARCHAR(16)  DEFAULT NULL,
  ref_id      BIGINT UNSIGNED DEFAULT NULL,
  read_flag   TINYINT(1)   NOT NULL DEFAULT 0,
  create_time DATETIME(6)  NOT NULL,
  update_time DATETIME(6)  NOT NULL,
  deleted     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_user_unread (user_id, read_flag, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信通知';

CREATE TABLE cf_field_def (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id  BIGINT UNSIGNED NOT NULL,
  field_key   VARCHAR(64)  NOT NULL,
  label       VARCHAR(128) NOT NULL,
  field_type  VARCHAR(16)  NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT/NUMBER/SELECT',
  options     VARCHAR(1024) DEFAULT NULL COMMENT 'SELECT 选项,JSON 数组',
  required    TINYINT(1)   NOT NULL DEFAULT 0,
  create_time DATETIME(6)  NOT NULL,
  update_time DATETIME(6)  NOT NULL,
  deleted     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_key (project_id, field_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷自定义字段定义';
