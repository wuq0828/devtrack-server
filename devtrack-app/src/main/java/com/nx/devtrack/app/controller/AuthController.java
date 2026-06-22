package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.config.FeishuProperties;
import com.nx.devtrack.app.manager.FeishuAuthManager;
import com.nx.devtrack.app.manager.UserManager;
import com.nx.devtrack.common.dto.LoginDto;
import com.nx.devtrack.common.request.LoginReq;
import com.nx.devtrack.common.web.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/devtrack/auth")
public class AuthController {

    private static final String FEISHU_AUTHORIZE = "https://open.feishu.cn/open-apis/authen/v1/authorize";

    private final UserManager userManager;
    private final FeishuAuthManager feishuAuthManager;
    private final FeishuProperties feishuProperties;

    @PostMapping("/login")
    public CommonResponse<LoginDto> login(@Valid @RequestBody LoginReq req) {
        return CommonResponse.ok(userManager.login(req.getUsername(), req.getPassword()));
    }

    /** 前端引导用户扫码:返回飞书授权 URL(dev 模式 appId 为空时返回提示) */
    @GetMapping("/feishu/authorize-url")
    public CommonResponse<Map<String, String>> feishuAuthorizeUrl() {
        if (feishuProperties.getAppId() == null || feishuProperties.getAppId().isBlank()) {
            return CommonResponse.ok(Map.of(
                    "devMode", "true",
                    "hint", "未配置 appId,飞书登录处于 dev 模式:直接 POST /devtrack/auth/feishu/login?code=<任意openId>"));
        }
        String url = UriComponentsBuilder.fromHttpUrl(FEISHU_AUTHORIZE)
                .queryParam("app_id", feishuProperties.getAppId())
                .queryParam("redirect_uri", feishuProperties.getRedirectUri())
                .queryParam("state", "devtrack")
                .build().toUriString();
        return CommonResponse.ok(Map.of("authorizeUrl", url));
    }

    /** 扫码回调:前端拿到 code 后调本接口换取登录 token */
    @PostMapping("/feishu/login")
    public CommonResponse<LoginDto> feishuLogin(@RequestParam("code") String code) {
        return CommonResponse.ok(feishuAuthManager.loginByCode(code));
    }
}
