package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.manager.TagManager;
import com.nx.devtrack.app.util.UserContext;
import com.nx.devtrack.common.dto.TagDto;
import com.nx.devtrack.common.request.CreateTagReq;
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
@RequestMapping("/devtrack/tag")
public class TagController {

    private final TagManager tagManager;

    @PostMapping("/create")
    public CommonResponse<TagDto> create(@Valid @RequestBody CreateTagReq req) {
        return CommonResponse.ok(tagManager.createTag(req, UserContext.getCurrentUserId()));
    }

    @PostMapping("/list")
    public CommonResponse<List<TagDto>> list(@Valid @RequestBody ProjectScopeReq req) {
        return CommonResponse.ok(tagManager.listTags(req.getProjectId(), UserContext.getCurrentUserId()));
    }
}
