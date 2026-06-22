package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 项目维度的请求(看板 / 统计)。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ProjectScopeReq extends BaseInnerRequest {

    @NotNull
    private Long projectId;
}
