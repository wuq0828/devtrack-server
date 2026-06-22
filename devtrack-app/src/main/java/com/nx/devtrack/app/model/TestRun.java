package com.nx.devtrack.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 测试执行轮次(test_run)。一次测试执行,圈定一批用例并记录每条结果。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "test_run")
@SQLRestriction("deleted = false")
public class TestRun extends BaseModel {

    private Long projectId;

    private String name;

    /** ACTIVE / CLOSED */
    private String status = "ACTIVE";

    private Long createdBy;
}
