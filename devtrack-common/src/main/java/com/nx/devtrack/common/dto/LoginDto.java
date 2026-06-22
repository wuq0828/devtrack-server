package com.nx.devtrack.common.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class LoginDto implements Serializable {

    private Long userId;
    private String username;
    /** 登录令牌,后续请求放入 Authorization: Bearer {token} */
    private String token;
    private boolean admin;
}
