package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

@EqualsAndHashCode(callSuper = true)
@Data
public class CreateIterationReq extends BaseInnerRequest {

    @NotNull
    private Long projectId;

    @NotBlank
    private String name;

    private LocalDate startDate;

    private LocalDate endDate;
}
