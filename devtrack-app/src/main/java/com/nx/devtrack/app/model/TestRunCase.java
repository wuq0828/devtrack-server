package com.nx.devtrack.app.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 轮次内的用例执行记录(test_run_case)。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "test_run_case")
@SQLRestriction("deleted = false")
public class TestRunCase extends BaseModel {

    private Long runId;

    private Long testCaseId;

    /** PENDING / PASS / FAIL / BLOCKED */
    private String status = "PENDING";

    private Long executedBy;
}
