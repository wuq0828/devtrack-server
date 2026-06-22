package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class DefectIdReq extends BaseInnerRequest {

    @NotNull
    private Long defectId;
}
