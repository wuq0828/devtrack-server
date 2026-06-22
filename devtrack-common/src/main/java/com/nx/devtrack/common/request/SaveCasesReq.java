package com.nx.devtrack.common.request;

import com.nx.devtrack.common.dto.GenCaseDto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class SaveCasesReq extends BaseInnerRequest {

    @NotNull
    private Long projectId;

    @NotEmpty
    private List<GenCaseDto> cases;
}
