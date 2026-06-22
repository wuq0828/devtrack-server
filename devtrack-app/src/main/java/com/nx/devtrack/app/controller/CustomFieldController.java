package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.manager.CustomFieldManager;
import com.nx.devtrack.app.util.UserContext;
import com.nx.devtrack.common.dto.CustomFieldDefDto;
import com.nx.devtrack.common.request.CreateCustomFieldReq;
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
@RequestMapping("/devtrack/customfield")
public class CustomFieldController {

    private final CustomFieldManager customFieldManager;

    @PostMapping("/create")
    public CommonResponse<CustomFieldDefDto> create(@Valid @RequestBody CreateCustomFieldReq req) {
        return CommonResponse.ok(customFieldManager.toDto(
                customFieldManager.createDef(req, UserContext.getCurrentUserId())));
    }

    @PostMapping("/defs")
    public CommonResponse<List<CustomFieldDefDto>> defs(@Valid @RequestBody ProjectScopeReq req) {
        return CommonResponse.ok(customFieldManager.listDefs(req.getProjectId(), UserContext.getCurrentUserId()));
    }
}
