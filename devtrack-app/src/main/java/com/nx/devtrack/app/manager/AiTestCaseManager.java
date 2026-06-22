package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.ai.ClaudeTestCaseGenerator;
import com.nx.devtrack.app.ai.FeishuDocReader;
import com.nx.devtrack.app.ai.GenArtifacts;
import com.nx.devtrack.app.ai.HeuristicTestCaseGenerator;
import com.nx.devtrack.app.security.PermissionManager;
import com.nx.devtrack.app.security.Perms;
import com.nx.devtrack.common.dto.GenCaseDto;
import com.nx.devtrack.common.dto.GenCasesResultDto;
import com.nx.devtrack.common.request.CreateTestCaseReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 提效:PRD 拆分 + 生成用例(Claude 真模型,无 key 兜底启发式)+ 评审入库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiTestCaseManager {

    private final ClaudeTestCaseGenerator claudeGenerator;
    private final HeuristicTestCaseGenerator heuristicGenerator;
    private final TestCaseManager testCaseManager;
    private final PermissionManager permissionManager;
    private final FeishuDocReader feishuDocReader;

    public GenCasesResultDto genCases(Long projectId, String prd, Long userId) {
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, projectId);

        // 若传入的是飞书文档链接,先读取其真实正文作为 PRD。
        if (feishuDocReader.isFeishuUrl(prd)) {
            log.info("[AI] 检测到飞书链接,读取文档正文作为 PRD");
            prd = feishuDocReader.readContent(prd);
        }

        if (claudeGenerator.available()) {
            try {
                GenArtifacts art = claudeGenerator.generate(prd);
                if (art.cases() != null && !art.cases().isEmpty()) {
                    return toResult("claude", art, prd);
                }
                log.warn("[AI] Claude 返回空,回退启发式");
            } catch (Exception e) {
                log.warn("[AI] Claude 调用失败,回退启发式: {}", e.getMessage());
            }
        }
        return toResult("heuristic", heuristicGenerator.generate(prd), prd);
    }

    /**
     * 组装返回结果。若模型未给出流程图/脑图(claude 偶发省略),用启发式生成的确定性图回填,
     * 保证前端总能拿到两张可渲染的图。
     */
    private GenCasesResultDto toResult(String engine, GenArtifacts art, String prd) {
        GenCasesResultDto dto = new GenCasesResultDto();
        dto.setEngine(engine);
        dto.setCases(art.cases());

        String flowchart = art.flowchart();
        String mindmap = art.mindmap();
        if (flowchart == null || mindmap == null) {
            GenArtifacts fallback = heuristicGenerator.generate(prd);
            if (flowchart == null) {
                flowchart = fallback.flowchart();
            }
            if (mindmap == null) {
                mindmap = fallback.mindmap();
            }
        }
        dto.setFlowchart(flowchart);
        dto.setMindmap(mindmap);
        return dto;
    }

    /** 评审入库:把(用户勾选后的)用例草稿批量建为正式测试用例 */
    @Transactional
    public int saveCases(Long projectId, List<GenCaseDto> cases, Long userId) {
        permissionManager.checkPermission(userId, Perms.BUG_CREATE, projectId);
        int created = 0;
        for (GenCaseDto c : cases) {
            if (c.getTitle() == null || c.getTitle().isBlank()) {
                continue;
            }
            CreateTestCaseReq req = new CreateTestCaseReq();
            req.setProjectId(projectId);
            req.setTitle(c.getTitle());
            req.setPreconditions(c.getPreconditions());
            req.setSteps(c.getSteps());
            req.setExpected(c.getExpected());
            testCaseManager.create(req, userId);
            created++;
        }
        return created;
    }
}
