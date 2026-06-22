-- ============================================================
-- V2:需求 + 测试用例(MySQL)。dt_relation 已在 V1 创建。
-- ============================================================

CREATE TABLE dt_requirement (
  id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id   BIGINT UNSIGNED NOT NULL,
  title        VARCHAR(255) NOT NULL,
  description  MEDIUMTEXT,
  status       VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/IN_PROGRESS/DONE',
  priority     VARCHAR(8)   NOT NULL DEFAULT 'P2',
  iteration_id BIGINT UNSIGNED DEFAULT NULL,
  reporter_id  BIGINT UNSIGNED DEFAULT NULL,
  create_time  DATETIME(6)  NOT NULL,
  update_time  DATETIME(6)  NOT NULL,
  deleted      TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_project (project_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求';

CREATE TABLE qa_test_case (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id     BIGINT UNSIGNED NOT NULL,
  title          VARCHAR(255) NOT NULL,
  preconditions  VARCHAR(4000),
  steps          MEDIUMTEXT,
  expected       VARCHAR(4000),
  status         VARCHAR(16)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PASS/FAIL/BLOCKED',
  automation_key VARCHAR(512) DEFAULT NULL COMMENT '对应 pytest fullName',
  create_time    DATETIME(6)  NOT NULL,
  update_time    DATETIME(6)  NOT NULL,
  deleted        TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_project (project_id, status),
  KEY idx_automation_key (automation_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测试用例';
