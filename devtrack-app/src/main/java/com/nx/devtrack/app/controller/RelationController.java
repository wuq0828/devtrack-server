package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.manager.RelationManager;
import com.nx.devtrack.app.util.UserContext;
import com.nx.devtrack.common.dto.RelationDto;
import com.nx.devtrack.common.request.LinkReq;
import com.nx.devtrack.common.request.RelationQueryReq;
import com.nx.devtrack.common.web.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 三向关联:需求 ↔ 缺陷 ↔ 用例。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/devtrack/relation")
public class RelationController {

    private final RelationManager relationManager;

    @PostMapping("/link")
    public CommonResponse<RelationDto> link(@Valid @RequestBody LinkReq req) {
        return CommonResponse.ok(relationManager.link(req, UserContext.getCurrentUserId()));
    }

    @PostMapping("/unlink")
    public CommonResponse<Void> unlink(@RequestBody Map<String, Long> body) {
        relationManager.unlink(body.get("relationId"), UserContext.getCurrentUserId());
        return CommonResponse.ok();
    }

    @PostMapping("/list")
    public CommonResponse<List<RelationDto>> list(@Valid @RequestBody RelationQueryReq req) {
        return CommonResponse.ok(
                relationManager.listRelated(req.getEntityType(), req.getEntityId(), UserContext.getCurrentUserId()));
    }
}
