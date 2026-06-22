package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class TransitionDefectReq extends BaseInnerRequest {

    @NotNull
    private Long defectId;

    /** 流转动作码,对应 wf_transition.code,如 confirm/start/resolve/verify/close/reject/reopen */
    @NotNull
    private String transitionCode;

    /** 流转备注;reject/reopen 等流转强制要求填写 */
    private String comment;
}
