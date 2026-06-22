package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.DefectDao;
import com.nx.devtrack.app.model.Defect;
import com.nx.devtrack.app.security.PermissionManager;
import com.nx.devtrack.app.security.Perms;
import com.nx.devtrack.common.dto.SlaItemDto;
import com.nx.devtrack.common.enums.Priority;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * SLA 超期预警:未关闭缺陷超过按优先级设定的处理时限即告警。
 * 生产可接 xxl-job 定时扫描 + 飞书推送;此处提供查询接口。
 */
@Service
@RequiredArgsConstructor
public class SlaManager {

    /** 各优先级 SLA 处理时限(天) */
    private static final Map<Priority, Long> SLA_DAYS = Map.of(
            Priority.P0, 1L, Priority.P1, 3L, Priority.P2, 7L, Priority.P3, 14L);

    /** 视为"已结束"的状态,不计入超期 */
    private static final Set<String> TERMINAL = Set.of("CLOSED", "REJECTED");

    private final DefectDao defectDao;
    private final PermissionManager permissionManager;

    public List<SlaItemDto> overdue(Long projectId, Long userId) {
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, projectId);
        LocalDateTime now = LocalDateTime.now();
        List<SlaItemDto> items = new ArrayList<>();
        for (Defect d : defectDao.findByProjectId(projectId)) {
            if (d.getStatusCode() != null && TERMINAL.contains(d.getStatusCode())) {
                continue;
            }
            if (d.getCreateTime() == null) {
                continue;
            }
            long slaDays = SLA_DAYS.getOrDefault(d.getPriority(), 7L);
            long ageDays = Math.max(0, Duration.between(d.getCreateTime(), now).toDays());
            if (ageDays <= slaDays) {
                continue;
            }
            SlaItemDto item = new SlaItemDto();
            item.setDefectId(d.getId());
            item.setDisplayKey("P" + d.getProjectId() + "-" + d.getSeqInProject());
            item.setTitle(d.getTitle());
            item.setPriority(d.getPriority() == null ? null : d.getPriority().name());
            item.setStatusCode(d.getStatusCode());
            item.setSlaDays(slaDays);
            item.setOverdueDays(ageDays - slaDays);
            items.add(item);
        }
        items.sort(Comparator.comparingLong(SlaItemDto::getOverdueDays).reversed());
        return items;
    }
}
