package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.DefectDao;
import com.nx.devtrack.app.model.Defect;
import com.nx.devtrack.common.enums.DefectSource;
import com.nx.devtrack.common.enums.Priority;
import com.nx.devtrack.common.enums.Severity;
import com.nx.devtrack.common.request.TestReportReq;
import com.nx.devtrack.common.request.TestResultItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 自动化测试报告处理:pytest/Allure 失败用例自动建缺陷,用 historyId 指纹去重。
 *
 * 幂等去重是命根子(技术方案 §8.2):同一失败反复跑不重复建单。
 * 大面积红(失败率过高,疑似环境挂)只建 1 个汇总单,不逐条刷屏。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutomationManager {

    /** 失败率阈值:超过则判定为环境性大面积失败,只建汇总单 */
    private static final double MASS_FAILURE_RATIO = 0.30;

    private final DefectDao defectDao;
    private final FeishuNotifyManager feishuNotifyManager;

    @Transactional
    public Map<String, Object> ingest(TestReportReq req) {
        Long projectId = resolveProjectId(req.getProjectCode());

        List<TestResultItem> failed = req.getResults().stream()
                .filter(r -> "failed".equalsIgnoreCase(r.getStatus()) || "broken".equalsIgnoreCase(r.getStatus()))
                .toList();

        int created = 0;
        int deduped = 0;

        if (!req.isAutoCreateDefect() || failed.isEmpty()) {
            return result(req, failed.size(), 0, 0, false);
        }

        boolean massFailure = (double) failed.size() / Math.max(1, req.getResults().size()) >= MASS_FAILURE_RATIO
                && req.getResults().size() >= 5;
        if (massFailure) {
            // 只建 1 个汇总单
            String fingerprint = "allure:massfail:" + req.getCiRunId();
            if (findOpenByFingerprint(projectId, fingerprint) == null) {
                Defect d = newAutoDefect(projectId,
                        String.format("[自动化][大面积失败] %d/%d 用例失败 @ %s",
                                failed.size(), req.getResults().size(), req.getCiRunId()),
                        buildMassDesc(req, failed), fingerprint);
                defectDao.save(d);
                feishuNotifyManager.notifyDefectChanged(d, "自动建单(大面积失败)", "allure-bot");
                created = 1;
            } else {
                deduped = 1;
            }
            return result(req, failed.size(), created, deduped, true);
        }

        for (TestResultItem item : failed) {
            String fingerprint = "allure:" + item.getHistoryId();
            Defect existing = findOpenByFingerprint(projectId, fingerprint);
            if (existing != null) {
                // 已有未关闭缺陷 -> 不重建(生产:追加一条「再次失败于 ciRun」评论)
                log.info("[Automation] 命中去重,跳过建单。fingerprint={}, defect#{}", fingerprint, existing.getId());
                deduped++;
                continue;
            }
            Defect d = newAutoDefect(projectId,
                    "[自动化失败] " + item.getFullName(),
                    buildSingleDesc(req, item), fingerprint);
            defectDao.save(d);
            feishuNotifyManager.notifyDefectChanged(d, "自动建单", "allure-bot");
            created++;
        }
        return result(req, failed.size(), created, deduped, false);
    }

    private Defect findOpenByFingerprint(Long projectId, String fingerprint) {
        return defectDao.findFirstByProjectIdAndExternalRefAndStatusCodeNot(projectId, fingerprint, "CLOSED");
    }

    private Defect newAutoDefect(Long projectId, String title, String desc, String fingerprint) {
        Defect d = new Defect();
        d.setProjectId(projectId);
        d.setSeqInProject(nextSeq(projectId));
        d.setTitle(title.length() > 250 ? title.substring(0, 250) : title);
        d.setDescription(desc);
        d.setStatusCode("NEW");
        d.setWorkflowId(1L);
        d.setSeverity(Severity.MAJOR);
        d.setPriority(Priority.P1);
        d.setSource(DefectSource.AUTOMATION);
        d.setExternalRef(fingerprint);
        return d;
    }

    private String buildSingleDesc(TestReportReq req, TestResultItem item) {
        return "用例: " + item.getFullName()
                + "\nhistoryId: " + item.getHistoryId()
                + "\nCI: " + req.getCiRunId()
                + "\nAllure: " + req.getAllureUrl()
                + "\n\n错误信息:\n" + (item.getErrorMessage() == null ? "(无)" : item.getErrorMessage());
    }

    private String buildMassDesc(TestReportReq req, List<TestResultItem> failed) {
        StringBuilder sb = new StringBuilder();
        sb.append("疑似环境性大面积失败,失败用例清单:\n");
        failed.stream().limit(50).forEach(f -> sb.append("- ").append(f.getFullName()).append("\n"));
        sb.append("\nCI: ").append(req.getCiRunId()).append("\nAllure: ").append(req.getAllureUrl());
        return sb.toString();
    }

    private int nextSeq(Long projectId) {
        Defect last = defectDao.findFirstByProjectIdOrderBySeqInProjectDesc(projectId);
        return last == null || last.getSeqInProject() == null ? 1 : last.getSeqInProject() + 1;
    }

    /**
     * 演示用:projectCode -> projectId 固定映射。
     * 生产从 dt_project 表按 code 查(phase 2 项目模块)。
     */
    private Long resolveProjectId(String projectCode) {
        return 1L;
    }

    private Map<String, Object> result(TestReportReq req, int failedCount, int created, int deduped, boolean massFailure) {
        Map<String, Object> m = new HashMap<>();
        m.put("totalResults", req.getResults().size());
        m.put("failedCount", failedCount);
        m.put("defectCreated", created);
        m.put("deduped", deduped);
        m.put("massFailure", massFailure);
        return m;
    }
}
