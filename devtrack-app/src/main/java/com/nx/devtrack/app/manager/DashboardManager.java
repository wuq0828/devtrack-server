package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.ActivityLogDao;
import com.nx.devtrack.common.dto.ActivityDto;
import com.nx.devtrack.common.dto.DashboardDto;
import com.nx.devtrack.common.dto.StatsDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.util.Map;

/**
 * 数据大屏聚合:复用缺陷统计 + 最近操作动态。
 */
@Service
@RequiredArgsConstructor
public class DashboardManager {

    private final DefectManager defectManager;
    private final ActivityLogDao activityLogDao;
    private final UserNameResolver userNameResolver;

    public DashboardDto overview(Long projectId, Long userId) {
        StatsDto stats = defectManager.stats(projectId, userId); // 内含 BUG_VIEW 鉴权
        Map<String, Long> byStatus = stats.getByStatus();
        long closed = byStatus.getOrDefault("CLOSED", 0L) + byStatus.getOrDefault("REJECTED", 0L);

        DashboardDto dto = new DashboardDto();
        dto.setTotal(stats.getTotal());
        dto.setOpenCount(Math.max(0, stats.getTotal() - closed));
        dto.setByStatus(byStatus);
        dto.setByPriority(stats.getByPriority());
        dto.setRecentActivities(activityLogDao.findTop20ByOrderByCreateTimeDesc().stream().map(a -> {
            ActivityDto ad = new ActivityDto();
            ad.setAction(a.getAction());
            ad.setDetail(a.getDetail());
            ad.setOperatorName(userNameResolver.name(a.getOperatorId()));
            ad.setCreateTime(a.getCreateTime() == null ? null
                    : a.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
            return ad;
        }).toList());
        return dto;
    }
}
