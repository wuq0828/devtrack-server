package com.nx.devtrack.common.request;

import com.nx.devtrack.common.enums.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CreateRequirementReq extends BaseInnerRequest {

    @NotNull
    private Long projectId;

    @NotBlank
    private String title;

    private String description;

    private Priority priority = Priority.P2;
}
