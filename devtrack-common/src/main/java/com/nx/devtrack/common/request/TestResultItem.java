package com.nx.devtrack.common.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 单条自动化用例结果,对应 Allure result 的关键字段。
 */
@Data
public class TestResultItem implements Serializable {

    /** Allure fullName,作为用例唯一标识(= qa_test_case.automation_key) */
    private String fullName;

    /** Allure historyId,跨次跑批稳定,作为自动建单去重指纹 */
    private String historyId;

    /** passed / failed / broken / skipped */
    private String status;

    private Long durationMs;

    /** 失败信息(失败/broken 时填) */
    private String errorMessage;
}
