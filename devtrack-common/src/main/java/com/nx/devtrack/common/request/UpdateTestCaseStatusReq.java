package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class UpdateTestCaseStatusReq extends BaseInnerRequest {

    @NotNull
    private Long testCaseId;

    /** PENDING / PASS / FAIL / BLOCKED */
    @NotBlank
    private String status;
}
