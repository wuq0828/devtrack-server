package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.manager.ApiTokenManager;
import com.nx.devtrack.app.security.PermissionManager;
import com.nx.devtrack.app.util.UserContext;
import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import com.nx.devtrack.common.web.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * API 机器令牌管理(仅 SYS_ADMIN)。创建返回明文,仅此一次。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/devtrack/apitoken")
public class ApiTokenController {

    private final ApiTokenManager apiTokenManager;
    private final PermissionManager permissionManager;

    @PostMapping("/create")
    public CommonResponse<Map<String, String>> create(@RequestBody Map<String, Object> body) {
        Long uid = UserContext.getCurrentUserId();
        if (!permissionManager.isSysAdmin(uid)) {
            throw new BizException(Errors.NO_PERMISSION);
        }
        String name = body.get("name") == null ? "unnamed" : String.valueOf(body.get("name"));
        String token = apiTokenManager.create(name, uid);
        return CommonResponse.ok(Map.of("name", name, "token", token));
    }
}
