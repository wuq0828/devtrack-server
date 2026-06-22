package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * pytest / CI 自动化测试报告上报请求。
 * 由 integration-pytest/devtrack_reporter.py 在会话结束时批量 POST。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class TestReportReq extends BaseInnerRequest {

    /** 项目编码,如 PAY */
    @NotBlank
    private String projectCode;

    /** 关联迭代 id,可空 */
    private Long iterationId;

    /** CI 运行标识,用于失败追溯 */
    private String ciRunId;

    /** Allure 报告链接 */
    private String allureUrl;

    @NotNull
    private List<TestResultItem> results;

    /** 失败用例是否自动建缺陷 */
    private boolean autoCreateDefect = true;
}
