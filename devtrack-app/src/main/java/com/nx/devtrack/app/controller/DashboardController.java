package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.manager.DashboardManager;
import com.nx.devtrack.app.util.UserContext;
import com.nx.devtrack.common.dto.DashboardDto;
import com.nx.devtrack.common.request.ProjectScopeReq;
import com.nx.devtrack.common.web.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/devtrack/dashboard")
public class DashboardController {

    private final DashboardManager dashboardManager;

    @PostMapping("/overview")
    public CommonResponse<DashboardDto> overview(@Valid @RequestBody ProjectScopeReq req) {
        return CommonResponse.ok(dashboardManager.overview(req.getProjectId(), UserContext.getCurrentUserId()));
    }
}
