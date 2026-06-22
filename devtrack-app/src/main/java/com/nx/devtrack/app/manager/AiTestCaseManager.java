package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.ai.ClaudeTestCaseGenerator;
import com.nx.devtrack.app.ai.HeuristicTestCaseGenerator;
import com.nx.devtrack.app.ai.TestCaseGenerator;
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

    public GenCasesResultDto genCases(Long projectId, String prd, Long userId) {
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, projectId);

        TestCaseGenerator generator = heuristicGenerator;
        String engine = "heuristic";
        if (claudeGenerator.available()) {
            try {
                List<GenCaseDto> cases = claudeGenerator.generate(prd);
                if (!cases.isEmpty()) {
                    GenCasesResultDto dto = new GenCasesResultDto();
                    dto.setEngine("claude");
                    dto.setCases(cases);
                    return dto;
                }
                log.warn("[AI] Claude 返回空,回退启发式");
            } catch (Exception e) {
                log.warn("[AI] Claude 调用失败,回退启发式: {}", e.getMessage());
            }
        }
        GenCasesResultDto dto = new GenCasesResultDto();
        dto.setEngine(engine);
        dto.setCases(generator.generate(prd));
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
