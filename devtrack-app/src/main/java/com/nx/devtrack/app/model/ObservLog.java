package com.nx.devtrack.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 观测云日志(observ_log)。从 CI/线上推送的崩溃/错误日志,可一键转缺陷。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "observ_log")
@SQLRestriction("deleted = false")
public class ObservLog extends BaseModel {

    private Long projectId;

    private String source;

    /** INFO / WARN / ERROR */
    private String level = "ERROR";

    @Column(length = 4000)
    private String message;

    /** 已转的缺陷 id */
    private Long defectId;
}
