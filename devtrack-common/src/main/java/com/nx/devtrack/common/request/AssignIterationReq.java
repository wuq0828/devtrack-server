package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class AssignIterationReq extends BaseInnerRequest {

    @NotNull
    private Long defectId;

    /** 目标迭代;传 null 表示移出迭代 */
    private Long iterationId;
}
