package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.manager.SlaManager;
import com.nx.devtrack.app.util.UserContext;
import com.nx.devtrack.common.dto.SlaItemDto;
import com.nx.devtrack.common.request.ProjectScopeReq;
import com.nx.devtrack.common.web.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/devtrack/sla")
public class SlaController {

    private final SlaManager slaManager;

    @PostMapping("/overdue")
    public CommonResponse<Map<String, List<SlaItemDto>>> overdue(@Valid @RequestBody ProjectScopeReq req) {
        List<SlaItemDto> items = slaManager.overdue(req.getProjectId(), UserContext.getCurrentUserId());
        return CommonResponse.ok(Map.of("items", items));
    }
}
