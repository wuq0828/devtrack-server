package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.DefectDao;
import com.nx.devtrack.app.dao.IterationDao;
import com.nx.devtrack.app.dao.WfStateDao;
import com.nx.devtrack.app.model.Defect;
import com.nx.devtrack.app.model.Iteration;
import com.nx.devtrack.app.model.WfState;
import com.nx.devtrack.app.security.PermissionManager;
import com.nx.devtrack.app.security.Perms;
import com.nx.devtrack.common.dto.BurndownDto;
import com.nx.devtrack.common.dto.BurndownPointDto;
import com.nx.devtrack.common.dto.IterationDto;
import com.nx.devtrack.common.dto.IterationReportDto;
import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import com.nx.devtrack.common.request.CreateIterationReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 迭代域:迭代 CRUD + 燃尽图。
 */
@Service
@RequiredArgsConstructor
public class IterationManager {

    private final IterationDao iterationDao;
    private final DefectDao defectDao;
    private final WfStateDao wfStateDao;
    private final PermissionManager permissionManager;

    @Transactional
    public Iteration create(CreateIterationReq req, Long userId) {
        permissionManager.checkPermission(userId, Perms.PROJECT_MANAGE, req.getProjectId());
        Iteration it = new Iteration();
        it.setProjectId(req.getProjectId());
        it.setName(req.getName());
        it.setStartDate(req.getStartDate());
        it.setEndDate(req.getEndDate());
        it.setStatus("ACTIVE");
        return iterationDao.save(it);
    }

    public List<Iteration> list(Long projectId, Long userId) {
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, projectId);
        return iterationDao.findByProjectIdOrderByCreateTimeDesc(projectId);
    }

    @Transactional
    public Iteration close(Long iterationId, Long userId) {
        Iteration it = iterationDao.findById(iterationId)
                .orElseThrow(() -> new BizException(Errors.PROJECT_NOT_FOUND));
        permissionManager.checkPermission(userId, Perms.PROJECT_MANAGE, it.getProjectId());
        it.setStatus("CLOSED");
        return iterationDao.save(it);
    }

    public BurndownDto burndown(Long iterationId, Long userId) {
        Iteration it = iterationDao.findById(iterationId)
                .orElseThrow(() -> new BizException(Errors.PROJECT_NOT_FOUND));
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, it.getProjectId());

        List<Defect> defects = defectDao.findByIterationId(iterationId);
        int total = defects.size();

        LocalDate today = LocalDate.now();
        LocalDate start = it.getStartDate() != null ? it.getStartDate() : today;
        LocalDate plannedEnd = it.getEndDate() != null ? it.getEndDate() : today;
        // 实际线只画到今天(未来还没发生)
        LocalDate actualEnd = plannedEnd.isBefore(today) ? plannedEnd : today;
        if (actualEnd.isBefore(start)) {
            actualEnd = start;
        }

        List<BurndownPointDto> points = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(actualEnd); d = d.plusDays(1)) {
            final LocalDate day = d;
            long doneByDay = defects.stream().filter(def -> {
                LocalDate doneDate = doneDate(def);
                return doneDate != null && !doneDate.isAfter(day);
            }).count();
            BurndownPointDto p = new BurndownPointDto();
            p.setDate(day.toString());
            p.setRemaining((int) (total - doneByDay));
            points.add(p);
        }

        // 理想线:从 total 线性递减到 0
        int n = points.size();
        for (int i = 0; i < n; i++) {
            int ideal = n <= 1 ? 0 : (int) Math.round(total * (double) (n - 1 - i) / (n - 1));
            points.get(i).setIdealRemaining(ideal);
        }

        BurndownDto dto = new BurndownDto();
        dto.setIterationName(it.getName());
        dto.setTotal(total);
        dto.setPoints(points);
        return dto;
    }

    public IterationReportDto report(Long iterationId, Long userId) {
        Iteration it = iterationDao.findById(iterationId)
                .orElseThrow(() -> new BizException(Errors.PROJECT_NOT_FOUND));
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, it.getProjectId());

        Map<String, String> statusCategory = wfStateDao.findByWorkflowIdOrderBySortOrderAsc(1L)
                .stream().collect(Collectors.toMap(WfState::getCode, WfState::getCategory, (a, b) -> a));

        List<Defect> defects = defectDao.findByIterationId(iterationId);
        int total = defects.size();
        int done = 0;
        int inProgress = 0;
        int todo = 0;
        for (Defect d : defects) {
            String cat = statusCategory.getOrDefault(d.getStatusCode(), "IN_PROGRESS");
            switch (cat) {
                case "DONE" -> done++;
                case "INITIAL" -> todo++;
                case "REJECTED" -> done++; // 已拒绝也算出迭代(不再占用)
                default -> inProgress++;
            }
        }

        IterationReportDto dto = new IterationReportDto();
        dto.setTotal(total);
        dto.setDone(done);
        dto.setInProgress(inProgress);
        dto.setTodo(todo);
        dto.setCompletionRate(total == 0 ? 0 : Math.round(done * 1000.0 / total) / 10.0);
        dto.setByStatus(countBy(defects, Defect::getStatusCode));
        dto.setByPriority(countBy(defects, d -> d.getPriority() == null ? "UNSET" : d.getPriority().name()));
        return dto;
    }

    private Map<String, Long> countBy(List<Defect> defects, Function<Defect, String> key) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Defect d : defects) {
            m.merge(key.apply(d), 1L, Long::sum);
        }
        return m;
    }

    /** 缺陷"完成"时间:优先 resolvedAt,退化 closedAt */
    private LocalDate doneDate(Defect d) {
        LocalDateTime t = d.getResolvedAt() != null ? d.getResolvedAt() : d.getClosedAt();
        return t == null ? null : t.toLocalDate();
    }

    public IterationDto toDto(Iteration it) {
        IterationDto dto = new IterationDto();
        dto.setId(it.getId());
        dto.setProjectId(it.getProjectId());
        dto.setName(it.getName());
        dto.setStatus(it.getStatus());
        dto.setStartDate(it.getStartDate());
        dto.setEndDate(it.getEndDate());
        return dto;
    }
}
