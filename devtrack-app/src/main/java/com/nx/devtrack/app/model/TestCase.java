package com.nx.devtrack.app.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.SQLRestriction;

/**
 * 测试用例(qa_test_case)。automationKey 与 pytest 用例 fullName 对应,
 * 自动化失败建单时可回填 case 关联(见 AutomationManager)。
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Entity
@Table(name = "qa_test_case")
@SQLRestriction("deleted = false")
public class TestCase extends BaseModel {

    private Long projectId;

    private String title;

    @Column(length = 4000)
    private String preconditions;

    @Column(length = 8000)
    private String steps;

    @Column(length = 4000)
    private String expected;

    /** PENDING / PASS / FAIL / BLOCKED */
    private String status = "PENDING";

    /** 对应 pytest 用例 fullName,可空 */
    @Column(length = 512)
    private String automationKey;

    /** 是否回归用例 */
    private boolean regression;
}
