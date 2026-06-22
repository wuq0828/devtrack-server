package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.manager.RequirementManager;
import com.nx.devtrack.app.util.UserContext;
import com.nx.devtrack.common.dto.RequirementDto;
import com.nx.devtrack.common.request.CreateRequirementReq;
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
@RequestMapping("/devtrack/requirement")
public class RequirementController {

    private final RequirementManager requirementManager;

    @PostMapping("/create")
    public CommonResponse<RequirementDto> create(@Valid @RequestBody CreateRequirementReq req) {
        return CommonResponse.ok(requirementManager.toDto(
                requirementManager.create(req, UserContext.getCurrentUserId())));
    }

    @PostMapping("/list")
    public CommonResponse<List<RequirementDto>> list(@Valid @RequestBody ProjectScopeReq req) {
        List<RequirementDto> list = requirementManager.list(req.getProjectId(), UserContext.getCurrentUserId())
                .stream().map(requirementManager::toDto).toList();
        return CommonResponse.ok(list);
    }
}
