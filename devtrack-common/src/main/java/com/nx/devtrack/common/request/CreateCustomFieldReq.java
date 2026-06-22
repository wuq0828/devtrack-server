package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class CreateCustomFieldReq extends BaseInnerRequest {

    @NotNull
    private Long projectId;

    @NotBlank
    private String fieldKey;

    @NotBlank
    private String label;

    /** TEXT / NUMBER / SELECT */
    private String fieldType = "TEXT";

    private List<String> options;

    private boolean required;
}
