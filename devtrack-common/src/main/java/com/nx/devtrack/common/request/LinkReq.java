package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 建立/解除 三向关联。type 取值:REQUIREMENT / DEFECT / TEST_CASE。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class LinkReq extends BaseInnerRequest {

    @NotBlank
    private String sourceType;
    @NotNull
    private Long sourceId;
    @NotBlank
    private String targetType;
    @NotNull
    private Long targetId;
    /** COVERS / VERIFIES / CAUSED_BY / RELATES / BLOCKS / DUPLICATES */
    @NotBlank
    private String relationType;
}
