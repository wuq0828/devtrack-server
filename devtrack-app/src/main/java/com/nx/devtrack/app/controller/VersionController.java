package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.manager.VersionManager;
import com.nx.devtrack.app.util.UserContext;
import com.nx.devtrack.common.dto.VersionDto;
import com.nx.devtrack.common.request.CreateVersionReq;
import com.nx.devtrack.common.request.ProjectScopeReq;
import com.nx.devtrack.common.web.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/devtrack/version")
public class VersionController {

    private final VersionManager versionManager;

    @PostMapping("/create")
    public CommonResponse<VersionDto> create(@Valid @RequestBody CreateVersionReq req) {
        return CommonResponse.ok(versionManager.toDto(versionManager.create(req, UserContext.getCurrentUserId())));
    }

    @PostMapping("/list")
    public CommonResponse<List<VersionDto>> list(@Valid @RequestBody ProjectScopeReq req) {
        return CommonResponse.ok(versionManager.list(req.getProjectId(), UserContext.getCurrentUserId())
                .stream().map(versionManager::toDto).toList());
    }
}
