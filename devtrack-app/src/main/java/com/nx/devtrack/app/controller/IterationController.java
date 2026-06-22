package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.manager.IterationManager;
import com.nx.devtrack.app.util.UserContext;
import com.nx.devtrack.common.dto.BurndownDto;
import com.nx.devtrack.common.dto.IterationDto;
import com.nx.devtrack.common.dto.IterationReportDto;
import com.nx.devtrack.common.request.CreateIterationReq;
import com.nx.devtrack.common.request.IterationIdReq;
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
@RequestMapping("/devtrack/iteration")
public class IterationController {

    private final IterationManager iterationManager;

    @PostMapping("/create")
    public CommonResponse<IterationDto> create(@Valid @RequestBody CreateIterationReq req) {
        return CommonResponse.ok(iterationManager.toDto(
                iterationManager.create(req, UserContext.getCurrentUserId())));
    }

    @PostMapping("/list")
    public CommonResponse<List<IterationDto>> list(@Valid @RequestBody ProjectScopeReq req) {
        List<IterationDto> list = iterationManager.list(req.getProjectId(), UserContext.getCurrentUserId())
                .stream().map(iterationManager::toDto).toList();
        return CommonResponse.ok(list);
    }

    @PostMapping("/close")
    public CommonResponse<IterationDto> close(@Valid @RequestBody IterationIdReq req) {
        return CommonResponse.ok(iterationManager.toDto(
                iterationManager.close(req.getIterationId(), UserContext.getCurrentUserId())));
    }

    @PostMapping("/burndown")
    public CommonResponse<BurndownDto> burndown(@Valid @RequestBody IterationIdReq req) {
        return CommonResponse.ok(iterationManager.burndown(req.getIterationId(), UserContext.getCurrentUserId()));
    }

    @PostMapping("/report")
    public CommonResponse<IterationReportDto> report(@Valid @RequestBody IterationIdReq req) {
        return CommonResponse.ok(iterationManager.report(req.getIterationId(), UserContext.getCurrentUserId()));
    }
}
