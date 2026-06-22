package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.manager.AiTestCaseManager;
import com.nx.devtrack.app.util.UserContext;
import com.nx.devtrack.common.dto.GenCasesResultDto;
import com.nx.devtrack.common.request.GenCasesReq;
import com.nx.devtrack.common.request.SaveCasesReq;
import com.nx.devtrack.common.web.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * AI 提效:从 PRD 生成测试用例 + 评审入库。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/devtrack/ai")
public class AiController {

    private final AiTestCaseManager aiTestCaseManager;

    @PostMapping("/gen-cases")
    public CommonResponse<GenCasesResultDto> genCases(@Valid @RequestBody GenCasesReq req) {
        return CommonResponse.ok(aiTestCaseManager.genCases(req.getProjectId(), req.getPrd(), UserContext.getCurrentUserId()));
    }

    @PostMapping("/save-cases")
    public CommonResponse<Map<String, Integer>> saveCases(@Valid @RequestBody SaveCasesReq req) {
        int created = aiTestCaseManager.saveCases(req.getProjectId(), req.getCases(), UserContext.getCurrentUserId());
        return CommonResponse.ok(Map.of("created", created));
    }
}
