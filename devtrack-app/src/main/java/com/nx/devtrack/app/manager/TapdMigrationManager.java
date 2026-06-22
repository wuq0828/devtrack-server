package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.DefectDao;
import com.nx.devtrack.app.model.Defect;
import com.nx.devtrack.common.enums.DefectSource;
import com.nx.devtrack.common.enums.Priority;
import com.nx.devtrack.common.enums.Severity;
import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * TAPD 数据迁移(技术方案 §10.3,go/no-go 隐形判据)。
 * 解析 TAPD 导出的 CSV,按字段映射表转成 DevTrack 缺陷,按 tapd:id 幂等去重。
 *
 * 迁移保真度策略:小批量试迁移 + 字段映射 + 人工抽检;映射不中的状态记录到 report 供核对。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TapdMigrationManager {

    /** TAPD 状态 -> DevTrack status_code */
    private static final Map<String, String> STATUS_MAP = Map.ofEntries(
            Map.entry("新", "NEW"), Map.entry("新建", "NEW"),
            Map.entry("已确认", "CONFIRMED"), Map.entry("确认", "CONFIRMED"),
            Map.entry("接受/处理", "IN_PROGRESS"), Map.entry("处理中", "IN_PROGRESS"), Map.entry("接受", "IN_PROGRESS"),
            Map.entry("已解决", "RESOLVED"), Map.entry("已修复", "RESOLVED"),
            Map.entry("已验证", "VERIFIED"), Map.entry("验证通过", "VERIFIED"),
            Map.entry("已关闭", "CLOSED"), Map.entry("关闭", "CLOSED"),
            Map.entry("已拒绝", "REJECTED"), Map.entry("拒绝", "REJECTED"),
            Map.entry("重新打开", "REOPENED"));

    private static final Map<String, Severity> SEVERITY_MAP = Map.ofEntries(
            Map.entry("致命", Severity.BLOCKER),
            Map.entry("严重", Severity.CRITICAL),
            Map.entry("一般", Severity.MAJOR),
            Map.entry("次要", Severity.MINOR), Map.entry("轻微", Severity.MINOR),
            Map.entry("提示", Severity.TRIVIAL));

    private static final Map<String, Priority> PRIORITY_MAP = Map.ofEntries(
            Map.entry("紧急", Priority.P0),
            Map.entry("高", Priority.P1),
            Map.entry("中", Priority.P2),
            Map.entry("低", Priority.P3));

    private final DefectDao defectDao;

    @Transactional
    public Map<String, Object> migrateCsv(String csvContent, Long projectId) {
        List<Map<String, String>> rows = parseCsv(csvContent);
        if (rows.isEmpty()) {
            throw new BizException(Errors.MIGRATION_FAILED.getCode(), "CSV 为空或无有效数据行");
        }

        int created = 0;
        int skipped = 0;
        List<String> unmappedStatus = new ArrayList<>();

        for (Map<String, String> row : rows) {
            String tapdId = first(row, "id", "ID", "缺陷ID", "编号");
            String title = first(row, "title", "标题", "缺陷标题", "name");
            if (title == null || title.isBlank()) {
                skipped++;
                continue;
            }
            String externalRef = "tapd:" + (tapdId == null ? title.hashCode() : tapdId);
            if (defectDao.existsByProjectIdAndExternalRef(projectId, externalRef)) {
                skipped++;
                continue;
            }

            String tapdStatus = first(row, "status", "状态");
            String statusCode = STATUS_MAP.get(tapdStatus == null ? "" : tapdStatus.trim());
            if (statusCode == null) {
                statusCode = "NEW";
                if (tapdStatus != null && !tapdStatus.isBlank()) {
                    unmappedStatus.add(tapdStatus);
                }
            }

            Defect d = new Defect();
            d.setProjectId(projectId);
            d.setSeqInProject(nextSeq(projectId));
            d.setTitle(title.length() > 250 ? title.substring(0, 250) : title);
            d.setDescription(first(row, "description", "详细描述", "描述", "重现步骤"));
            d.setStatusCode(statusCode);
            d.setWorkflowId(1L);
            d.setSeverity(SEVERITY_MAP.getOrDefault(trim(first(row, "severity", "严重程度")), Severity.MAJOR));
            d.setPriority(PRIORITY_MAP.getOrDefault(trim(first(row, "priority", "优先级")), Priority.P2));
            d.setSource(DefectSource.MIGRATION);
            d.setExternalRef(externalRef);
            defectDao.save(d);
            created++;
        }

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalRows", rows.size());
        report.put("created", created);
        report.put("skipped", skipped);
        report.put("unmappedStatusSamples", unmappedStatus.stream().distinct().limit(20).toList());
        log.info("[Migration] project={} 共 {} 行,建 {},跳过 {}", projectId, rows.size(), created, skipped);
        return report;
    }

    private int nextSeq(Long projectId) {
        Defect last = defectDao.findFirstByProjectIdOrderBySeqInProjectDesc(projectId);
        return last == null || last.getSeqInProject() == null ? 1 : last.getSeqInProject() + 1;
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }

    /** 取第一个命中的列(支持中英文别名) */
    private String first(Map<String, String> row, String... keys) {
        for (String k : keys) {
            if (row.containsKey(k) && row.get(k) != null) {
                return row.get(k);
            }
        }
        return null;
    }

    /** 最小 CSV 解析:支持双引号包裹字段(内含逗号/换行)与 "" 转义 */
    static List<Map<String, String>> parseCsv(String content) {
        List<List<String>> records = splitRecords(content);
        List<Map<String, String>> rows = new ArrayList<>();
        if (records.isEmpty()) {
            return rows;
        }
        List<String> headers = records.get(0);
        for (int i = 1; i < records.size(); i++) {
            List<String> cells = records.get(i);
            if (cells.size() == 1 && cells.get(0).isBlank()) {
                continue;
            }
            Map<String, String> row = new HashMap<>();
            for (int c = 0; c < headers.size(); c++) {
                row.put(headers.get(c).trim(), c < cells.size() ? cells.get(c) : null);
            }
            rows.add(row);
        }
        return rows;
    }

    private static List<List<String>> splitRecords(String content) {
        List<List<String>> records = new ArrayList<>();
        List<String> current = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < content.length(); i++) {
            char ch = content.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < content.length() && content.charAt(i + 1) == '"') {
                        field.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    field.append(ch);
                }
            } else {
                switch (ch) {
                    case '"' -> inQuotes = true;
                    case ',' -> {
                        current.add(field.toString());
                        field.setLength(0);
                    }
                    case '\r' -> { /* ignore */ }
                    case '\n' -> {
                        current.add(field.toString());
                        field.setLength(0);
                        records.add(current);
                        current = new ArrayList<>();
                    }
                    default -> field.append(ch);
                }
            }
        }
        // 末尾未换行的最后一行
        if (field.length() > 0 || !current.isEmpty()) {
            current.add(field.toString());
            records.add(current);
        }
        return records;
    }
}
