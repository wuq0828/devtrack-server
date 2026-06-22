package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class SetVersionReq extends BaseInnerRequest {

    @NotNull
    private Long defectId;

    /** 修复版本 id;传 null 表示清除 */
    private Long versionId;
}
