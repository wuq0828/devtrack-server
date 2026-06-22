package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateTestCaseReq extends BaseInnerRequest {

    @NotNull
    private Long testCaseId;

    @NotBlank
    private String title;

    private String preconditions;

    private String steps;

    private String expected;

    /** 对应 pytest 用例 fullName,可空 */
    private String automationKey;
}
