package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class CreateTagReq extends BaseInnerRequest {

    @NotNull
    private Long projectId;

    @NotBlank
    private String name;

    private String color = "#409EFF";
}
