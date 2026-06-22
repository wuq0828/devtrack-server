-- ============================================================
-- V4:多端标签 + 版本(MySQL)
-- ============================================================

CREATE TABLE dt_tag (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id  BIGINT UNSIGNED NOT NULL,
  name        VARCHAR(64)  NOT NULL,
  color       VARCHAR(16)  NOT NULL DEFAULT '#409EFF',
  create_time DATETIME(6)  NOT NULL,
  update_time DATETIME(6)  NOT NULL,
  deleted     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签';

CREATE TABLE dt_entity_tag (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  entity_type VARCHAR(16)  NOT NULL DEFAULT 'DEFECT',
  entity_id   BIGINT UNSIGNED NOT NULL,
  tag_id      BIGINT UNSIGNED NOT NULL,
  create_time DATETIME(6)  NOT NULL,
  update_time DATETIME(6)  NOT NULL,
  deleted     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_entity (entity_type, entity_id),
  KEY idx_tag (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='标签挂载';

CREATE TABLE dt_version (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id  BIGINT UNSIGNED NOT NULL,
  name        VARCHAR(64)  NOT NULL,
  status      VARCHAR(16)  NOT NULL DEFAULT 'PLANNING' COMMENT 'PLANNING/RELEASED',
  create_time DATETIME(6)  NOT NULL,
  update_time DATETIME(6)  NOT NULL,
  deleted     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_project (project_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='版本';

ALTER TABLE dt_defect ADD COLUMN fix_version_id BIGINT UNSIGNED DEFAULT NULL COMMENT '修复版本';
