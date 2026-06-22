package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class BatchTransitionReq extends BaseInnerRequest {

    @NotEmpty
    private List<Long> defectIds;

    @NotNull
    private String transitionCode;

    private String comment;
}
