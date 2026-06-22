package com.nx.devtrack.app.model;

import com.nx.devtrack.common.enums.DefectSource;
import com.nx.devtrack.common.enums.Priority;
import com.nx.devtrack.common.enums.Severity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

/**
 * 缺陷聚合根。status_code 为字符串而非枚举——状态集由 wf_state 配置驱动,不硬编码。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "dt_defect")
@SQLRestriction("deleted = false")
public class Defect extends BaseModel {

    private Long projectId;

    /** 项目内序号,配项目 code 展示为 PAY-1024 */
    private Integer seqInProject;

    private String title;

    @Column(length = 8000)
    private String description;

    /** 引用 wf_state.code,默认初始态 NEW */
    private String statusCode = "NEW";

    private Long workflowId = 1L;

    @Enumerated(EnumType.STRING)
    private Severity severity = Severity.MAJOR;

    @Enumerated(EnumType.STRING)
    private Priority priority = Priority.P2;

    private Long reporterId;

    private Long assigneeId;

    private Long iterationId;

    /** 修复版本(关联 dt_version) */
    private Long fixVersionId;

    @Enumerated(EnumType.STRING)
    private DefectSource source = DefectSource.MANUAL;

    /** TAPD 原 ID 或 allure:historyId,迁移与自动建单去重用 */
    private String externalRef;

    /** 自定义字段值(JSON 字符串),定义见 cf_field_def */
    @Column(length = 8000)
    private String customFields;

    private LocalDateTime resolvedAt;

    private LocalDateTime closedAt;
}
