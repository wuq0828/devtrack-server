package com.nx.devtrack.common.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
public class LoginReq extends BaseInnerRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String password;
}
