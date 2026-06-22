package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class SetTagsReq extends BaseInnerRequest {

    @NotNull
    private Long defectId;

    /** 完整标签 id 列表(覆盖式设置);空列表表示清空 */
    @NotNull
    private List<Long> tagIds;
}
