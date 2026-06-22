package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 查询某实体的全部关联(作为 source 或 target 都算)。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class RelationQueryReq extends BaseInnerRequest {

    @NotBlank
    private String entityType;

    @NotNull
    private Long entityId;
}
