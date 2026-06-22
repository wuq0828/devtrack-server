package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class GenCasesReq extends BaseInnerRequest {

    @NotNull
    private Long projectId;

    /** PRD / 需求描述文本 */
    @NotBlank
    private String prd;
}
