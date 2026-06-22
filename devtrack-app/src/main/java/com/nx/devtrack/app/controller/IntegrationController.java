package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.manager.ApiTokenManager;
import com.nx.devtrack.app.manager.AutomationManager;
import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import com.nx.devtrack.common.request.TestReportReq;
import com.nx.devtrack.common.web.CommonResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 对外开放 API,供 pytest/CI 调用。走 API Token(机器账号),不走用户登录 token。
 * Token 库里只存 SHA-256(ApiTokenManager),明文仅创建时返回一次。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/devtrack/integrations")
public class IntegrationController {

    private final AutomationManager automationManager;
    private final ApiTokenManager apiTokenManager;

    @PostMapping("/test-reports")
    public CommonResponse<Map<String, Object>> ingest(@Valid @RequestBody TestReportReq req,
                                                       HttpServletRequest httpReq) {
        checkApiToken(httpReq);
        return CommonResponse.ok(automationManager.ingest(req));
    }

    private void checkApiToken(HttpServletRequest httpReq) {
        String header = httpReq.getHeader("Authorization");
        String token = header == null ? null : header.replace("Bearer ", "").trim();
        if (!apiTokenManager.verify(token)) {
            throw new BizException(Errors.NO_PERMISSION);
        }
    }
}
