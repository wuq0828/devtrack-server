package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
public class SetFieldsReq extends BaseInnerRequest {

    @NotNull
    private Long defectId;

    @NotNull
    private Map<String, Object> fields;
}
