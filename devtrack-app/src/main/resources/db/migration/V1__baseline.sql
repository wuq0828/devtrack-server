-- ============================================================
-- DevTrack 缺陷管理系统 基线 schema (MySQL 8.0)
-- 对齐技术方案 §5。仅生产 profile(Flyway)执行;本地 H2 由 JPA ddl-auto 自动建表。
-- 约定:所有业务表带 create_time/update_time/deleted(软删),主键 BIGINT。
-- ============================================================

-- ---------- 用户 / 权限 ----------
CREATE TABLE dt_user (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  username       VARCHAR(64)  NOT NULL,
  password       VARCHAR(128) NOT NULL COMMENT '生产存 BCrypt 哈希',
  display_name   VARCHAR(64)  DEFAULT NULL,
  feishu_open_id VARCHAR(64)  DEFAULT NULL COMMENT '飞书 open_id,用于 @ 与卡片推送',
  login_token    VARCHAR(64)  DEFAULT NULL,
  last_login_time DATETIME(6) DEFAULT NULL,
  admin          TINYINT(1)   NOT NULL DEFAULT 0,
  create_time    DATETIME(6)  NOT NULL,
  update_time    DATETIME(6)  NOT NULL,
  deleted        TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username),
  KEY idx_login_token (login_token)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户';

CREATE TABLE sys_role (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code        VARCHAR(32)  NOT NULL COMMENT 'SYS_ADMIN/PROJECT_OWNER/DEV/QA/PM/GUEST/SERVICE',
  name        VARCHAR(64)  NOT NULL,
  scope       VARCHAR(16)  NOT NULL DEFAULT 'PROJECT' COMMENT 'GLOBAL/PROJECT',
  create_time DATETIME(6)  NOT NULL,
  update_time DATETIME(6)  NOT NULL,
  deleted     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色';

CREATE TABLE sys_user_role (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  user_id     BIGINT UNSIGNED NOT NULL,
  role_id     BIGINT UNSIGNED NOT NULL,
  project_id  BIGINT UNSIGNED DEFAULT NULL COMMENT 'PROJECT 级角色生效的项目;GLOBAL 角色为 NULL',
  create_time DATETIME(6)  NOT NULL,
  update_time DATETIME(6)  NOT NULL,
  deleted     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_role_project (user_id, role_id, project_id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户-角色(带项目数据范围)';

-- ---------- 项目 / 迭代 ----------
CREATE TABLE dt_project (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  code        VARCHAR(16)  NOT NULL COMMENT '项目编码,展示编号前缀,如 PAY',
  name        VARCHAR(128) NOT NULL,
  owner_id    BIGINT UNSIGNED DEFAULT NULL,
  workflow_id BIGINT UNSIGNED NOT NULL DEFAULT 1,
  create_time DATETIME(6)  NOT NULL,
  update_time DATETIME(6)  NOT NULL,
  deleted     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='项目';

CREATE TABLE dt_iteration (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id  BIGINT UNSIGNED NOT NULL,
  name        VARCHAR(128) NOT NULL,
  status      VARCHAR(16)  NOT NULL DEFAULT 'PLANNING' COMMENT 'PLANNING/ACTIVE/CLOSED',
  start_date  DATE DEFAULT NULL,
  end_date    DATE DEFAULT NULL,
  create_time DATETIME(6)  NOT NULL,
  update_time DATETIME(6)  NOT NULL,
  deleted     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_project (project_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='迭代';

-- ---------- 缺陷(核心) ----------
CREATE TABLE dt_defect (
  id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id     BIGINT UNSIGNED NOT NULL,
  seq_in_project INT UNSIGNED  NOT NULL COMMENT '项目内序号,配 code 展示为 PAY-1024',
  title          VARCHAR(255) NOT NULL,
  description    MEDIUMTEXT,
  workflow_id    BIGINT UNSIGNED NOT NULL DEFAULT 1,
  status_code    VARCHAR(32)  NOT NULL DEFAULT 'NEW' COMMENT '引用 wf_state.code,不硬编码',
  severity       VARCHAR(16)  NOT NULL DEFAULT 'MAJOR',
  priority       VARCHAR(8)   NOT NULL DEFAULT 'P2',
  reporter_id    BIGINT UNSIGNED DEFAULT NULL,
  assignee_id    BIGINT UNSIGNED DEFAULT NULL,
  iteration_id   BIGINT UNSIGNED DEFAULT NULL,
  source         VARCHAR(16)  NOT NULL DEFAULT 'MANUAL' COMMENT 'MANUAL/AUTOMATION/MIGRATION',
  external_ref   VARCHAR(128) DEFAULT NULL COMMENT 'TAPD原ID / allure:historyId,迁移与去重',
  resolved_at    DATETIME(6)  DEFAULT NULL,
  closed_at      DATETIME(6)  DEFAULT NULL,
  create_time    DATETIME(6)  NOT NULL,
  update_time    DATETIME(6)  NOT NULL,
  deleted        TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_project_seq (project_id, seq_in_project),
  UNIQUE KEY uk_external_ref (external_ref),
  KEY idx_board    (project_id, status_code, priority, assignee_id),
  KEY idx_assignee (assignee_id, status_code),
  KEY idx_iter     (iteration_id, status_code),
  KEY idx_created  (project_id, create_time),
  FULLTEXT KEY ft_title_desc (title, description) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='缺陷';

-- ---------- 工作流(配置化状态机) ----------
CREATE TABLE wf_state (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  workflow_id BIGINT UNSIGNED NOT NULL,
  code        VARCHAR(32)  NOT NULL,
  name        VARCHAR(64)  NOT NULL,
  category    VARCHAR(16)  NOT NULL COMMENT 'INITIAL/IN_PROGRESS/DONE/REJECTED',
  is_initial  TINYINT(1)   NOT NULL DEFAULT 0,
  sort_order  SMALLINT     NOT NULL DEFAULT 0,
  create_time DATETIME(6)  NOT NULL,
  update_time DATETIME(6)  NOT NULL,
  deleted     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_state (workflow_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流状态节点';

CREATE TABLE wf_transition (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  workflow_id     BIGINT UNSIGNED NOT NULL,
  code            VARCHAR(32)  NOT NULL COMMENT 'confirm/start/resolve/verify/close/reject/reopen',
  name            VARCHAR(64)  NOT NULL,
  from_state      VARCHAR(32)  NOT NULL COMMENT '"*"=任意态',
  to_state        VARCHAR(32)  NOT NULL,
  require_role    VARCHAR(32)  DEFAULT NULL,
  require_comment TINYINT(1)   NOT NULL DEFAULT 0,
  hook_event      VARCHAR(64)  DEFAULT NULL,
  create_time     DATETIME(6)  NOT NULL,
  update_time     DATETIME(6)  NOT NULL,
  deleted         TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_transition (workflow_id, from_state, code),
  KEY idx_from (workflow_id, from_state)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工作流流转规则';

CREATE TABLE dt_status_history (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  entity_type   VARCHAR(16)  NOT NULL DEFAULT 'DEFECT',
  entity_id     BIGINT UNSIGNED NOT NULL,
  from_state    VARCHAR(32)  DEFAULT NULL,
  to_state      VARCHAR(32)  NOT NULL,
  transition_code VARCHAR(32) DEFAULT NULL,
  operator_id   BIGINT UNSIGNED DEFAULT NULL,
  duration_sec  BIGINT DEFAULT NULL COMMENT '在前一状态停留秒数,入库时算好,供报表用',
  comment       VARCHAR(1024) DEFAULT NULL,
  create_time   DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_entity (entity_type, entity_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='状态流转不可变流水';

-- ---------- 横切支撑(多态挂载) ----------
CREATE TABLE dt_comment (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  entity_type VARCHAR(16)  NOT NULL DEFAULT 'DEFECT',
  entity_id   BIGINT UNSIGNED NOT NULL,
  content     MEDIUMTEXT   NOT NULL,
  author_id   BIGINT UNSIGNED NOT NULL,
  create_time DATETIME(6)  NOT NULL,
  update_time DATETIME(6)  NOT NULL,
  deleted     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_entity (entity_type, entity_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评论';

CREATE TABLE dt_attachment (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  entity_type VARCHAR(16)  NOT NULL DEFAULT 'DEFECT',
  entity_id   BIGINT UNSIGNED NOT NULL,
  file_name   VARCHAR(255) NOT NULL,
  object_key  VARCHAR(512) NOT NULL COMMENT 'MinIO object key,绝不存公网直链',
  size_bytes  BIGINT DEFAULT NULL,
  uploader_id BIGINT UNSIGNED NOT NULL,
  create_time DATETIME(6)  NOT NULL,
  update_time DATETIME(6)  NOT NULL,
  deleted     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_entity (entity_type, entity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='附件';

CREATE TABLE dt_activity_log (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  entity_type VARCHAR(16)  NOT NULL DEFAULT 'DEFECT',
  entity_id   BIGINT UNSIGNED NOT NULL,
  action      VARCHAR(32)  NOT NULL,
  detail      MEDIUMTEXT COMMENT '字段级 diff(JSON)',
  operator_id BIGINT UNSIGNED DEFAULT NULL,
  create_time DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_entity (entity_type, entity_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作历史(审计)';

CREATE TABLE dt_relation (
  id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  source_type   VARCHAR(16) NOT NULL,
  source_id     BIGINT UNSIGNED NOT NULL,
  target_type   VARCHAR(16) NOT NULL,
  target_id     BIGINT UNSIGNED NOT NULL,
  relation_type VARCHAR(16) NOT NULL COMMENT 'COVERS/VERIFIES/BLOCKS/RELATES/DUPLICATES/CAUSED_BY',
  create_time   DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_relation (source_type, source_id, target_type, target_id, relation_type),
  KEY idx_target (target_type, target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='需求-缺陷-用例三向关联';

-- ---------- 自动化集成 ----------
CREATE TABLE intg_api_token (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  name        VARCHAR(64)  NOT NULL,
  token_hash  CHAR(64)     NOT NULL COMMENT '明文仅返回一次,只存 SHA-256',
  user_id     BIGINT UNSIGNED NOT NULL COMMENT '机器账号 user id',
  create_time DATETIME(6)  NOT NULL,
  update_time DATETIME(6)  NOT NULL,
  deleted     TINYINT(1)   NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_token_hash (token_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='开放 API 机器令牌';

CREATE TABLE intg_test_report (
  id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  project_id  BIGINT UNSIGNED NOT NULL,
  iteration_id BIGINT UNSIGNED DEFAULT NULL,
  ci_run_id   VARCHAR(128) DEFAULT NULL,
  allure_url  VARCHAR(512) DEFAULT NULL,
  total_count INT NOT NULL DEFAULT 0,
  failed_count INT NOT NULL DEFAULT 0,
  create_time DATETIME(6)  NOT NULL,
  PRIMARY KEY (id),
  KEY idx_project (project_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自动化测试报告';

CREATE TABLE qa_test_run (
  id              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  report_id       BIGINT UNSIGNED NOT NULL,
  automation_key  VARCHAR(512) NOT NULL COMMENT 'Allure fullName',
  allure_history_id VARCHAR(128) DEFAULT NULL,
  status          VARCHAR(16) NOT NULL,
  duration_ms     BIGINT DEFAULT NULL,
  defect_id       BIGINT UNSIGNED DEFAULT NULL COMMENT '失败自动建/命中的缺陷',
  create_time     DATETIME(6) NOT NULL,
  PRIMARY KEY (id),
  KEY idx_report (report_id),
  KEY idx_history (allure_history_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用例执行明细';

-- ---------- 默认工作流种子(workflowId=1) ----------
INSERT INTO wf_state (workflow_id, code, name, category, is_initial, sort_order, create_time, update_time, deleted) VALUES
 (1,'NEW','新建','INITIAL',1,10,NOW(6),NOW(6),0),
 (1,'CONFIRMED','已确认','IN_PROGRESS',0,20,NOW(6),NOW(6),0),
 (1,'IN_PROGRESS','处理中','IN_PROGRESS',0,30,NOW(6),NOW(6),0),
 (1,'RESOLVED','已解决','IN_PROGRESS',0,40,NOW(6),NOW(6),0),
 (1,'VERIFIED','已验证','IN_PROGRESS',0,50,NOW(6),NOW(6),0),
 (1,'CLOSED','关闭','DONE',0,60,NOW(6),NOW(6),0),
 (1,'REOPENED','重新打开','IN_PROGRESS',0,70,NOW(6),NOW(6),0),
 (1,'REJECTED','已拒绝','REJECTED',0,80,NOW(6),NOW(6),0);

INSERT INTO wf_transition (workflow_id, code, name, from_state, to_state, require_role, require_comment, create_time, update_time, deleted) VALUES
 (1,'confirm','确认','NEW','CONFIRMED',NULL,0,NOW(6),NOW(6),0),
 (1,'start','开始处理','CONFIRMED','IN_PROGRESS',NULL,0,NOW(6),NOW(6),0),
 (1,'start','开始处理','REOPENED','IN_PROGRESS',NULL,0,NOW(6),NOW(6),0),
 (1,'resolve','解决','IN_PROGRESS','RESOLVED',NULL,0,NOW(6),NOW(6),0),
 (1,'verify','验证通过','RESOLVED','VERIFIED','QA',0,NOW(6),NOW(6),0),
 (1,'close','关闭','VERIFIED','CLOSED',NULL,0,NOW(6),NOW(6),0),
 (1,'reopen','重新打开','RESOLVED','REOPENED',NULL,1,NOW(6),NOW(6),0),
 (1,'reopen','重新打开','VERIFIED','REOPENED',NULL,1,NOW(6),NOW(6),0),
 (1,'reopen','重新打开','CLOSED','REOPENED',NULL,1,NOW(6),NOW(6),0),
 (1,'reject','拒绝','*','REJECTED',NULL,1,NOW(6),NOW(6),0);

-- 默认演示项目 + admin(与 DataInitializer 幂等互补;生产可按需删除)
INSERT INTO dt_project (code, name, workflow_id, create_time, update_time, deleted)
 VALUES ('DEMO','演示项目',1,NOW(6),NOW(6),0);
