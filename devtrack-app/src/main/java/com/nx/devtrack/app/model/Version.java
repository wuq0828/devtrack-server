package com.nx.devtrack.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDate;

/**
 * 版本(dt_version)。缺陷可关联修复版本(fix_version_id)。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "dt_version")
@SQLRestriction("deleted = false")
public class Version extends BaseModel {

    private Long projectId;

    private String name;

    /** PLANNING / RELEASED */
    private String status = "PLANNING";

    /** 计划发布日期(甘特里程碑) */
    private LocalDate releaseDate;
}
