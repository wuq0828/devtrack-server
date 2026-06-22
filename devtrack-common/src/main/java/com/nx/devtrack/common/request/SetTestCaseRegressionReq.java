package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SetTestCaseRegressionReq extends BaseInnerRequest {

    @NotNull
    private Long testCaseId;

    /** 是否纳入回归套件 */
    @NotNull
    private Boolean regression;
}
