package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.ActivityLogDao;
import com.nx.devtrack.app.dao.DefectDao;
import com.nx.devtrack.app.dao.StatusHistoryDao;
import com.nx.devtrack.app.dao.WfStateDao;
import com.nx.devtrack.app.dao.WfTransitionDao;
import com.nx.devtrack.app.model.ActivityLog;
import com.nx.devtrack.app.model.Defect;
import com.nx.devtrack.app.model.StatusHistory;
import com.nx.devtrack.app.model.WfState;
import com.nx.devtrack.app.model.WfTransition;
import com.nx.devtrack.app.security.PermissionManager;
import com.nx.devtrack.app.security.Perms;
import com.nx.devtrack.common.dto.ActivityDto;
import com.nx.devtrack.common.dto.AvailableTransitionDto;
import com.nx.devtrack.common.dto.BatchResultDto;
import com.nx.devtrack.common.dto.BoardColumnDto;
import com.nx.devtrack.common.dto.DefectDetailDto;
import com.nx.devtrack.common.dto.DefectDto;
import com.nx.devtrack.common.dto.StatsDto;
import com.nx.devtrack.common.dto.StatusHistoryDto;
import com.nx.devtrack.common.enums.DefectSource;
import com.nx.devtrack.common.enums.Priority;
import com.nx.devtrack.common.enums.Severity;
import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import com.nx.devtrack.common.request.CreateDefectReq;
import com.nx.devtrack.common.request.ListDefectReq;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 缺陷域业务。状态流转读 wf_transition 表驱动(配置化状态机),不写 switch(status)。
 * Phase 2:流转写不可变历史 + 审计、缺陷详情、看板、统计、数据范围。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DefectManager {

    private static final String WILDCARD_STATE = "*";
    private static final String ENTITY_DEFECT = "DEFECT";
    private static final Long DEFAULT_WORKFLOW = 1L;

    private final DefectDao defectDao;
    private final WfTransitionDao wfTransitionDao;
    private final WfStateDao wfStateDao;
    private final StatusHistoryDao statusHistoryDao;
    private final ActivityLogDao activityLogDao;
    private final CommentManager commentManager;
    private final FeishuNotifyManager feishuNotifyManager;
    private final NotificationManager notificationManager;
    private final PermissionManager permissionManager;
    private final UserNameResolver userNameResolver;
    private final ObjectMapper objectMapper;

    @Transactional
    public Defect create(CreateDefectReq req, Long reporterId) {
        permissionManager.checkPermission(reporterId, Perms.BUG_CREATE, req.getProjectId());
        Defect defect = new Defect();
        defect.setProjectId(req.getProjectId());
        defect.setSeqInProject(nextSeq(req.getProjectId()));
        defect.setTitle(req.getTitle());
        defect.setDescription(req.getDescription());
        defect.setSeverity(req.getSeverity() == null ? Severity.MAJOR : req.getSeverity());
        defect.setPriority(req.getPriority() == null ? Priority.P2 : req.getPriority());
        defect.setReporterId(reporterId);
        defect.setAssigneeId(req.getAssigneeId());
        defect.setIterationId(req.getIterationId());
        defect.setStatusCode("NEW");
        defect.setWorkflowId(DEFAULT_WORKFLOW);
        defect.setSource(DefectSource.MANUAL);
        defectDao.save(defect);

        writeActivity(defect.getId(), "CREATE", "创建缺陷: " + defect.getTitle(), reporterId);
        feishuNotifyManager.notifyDefectChanged(defect, "新建", userNameResolver.name(reporterId));
        if (defect.getAssigneeId() != null && !defect.getAssigneeId().equals(reporterId)) {
            notificationManager.notify(defect.getAssigneeId(), "ASSIGN",
                    "缺陷 #" + defect.getId() + " 指派给你:" + defect.getTitle(), "DEFECT", defect.getId());
        }
        return defect;
    }

    /**
     * 状态流转(配置化):读 wf_transition 决定合法性,精确匹配优先,再尝试通配(from='*')。
     */
    @Transactional
    public Defect transition(Long defectId, String transitionCode, String comment, Long operatorId) {
        Defect defect = defectDao.findById(defectId)
                .orElseThrow(() -> new BizException(Errors.DEFECT_NOT_FOUND));

        permissionManager.checkPermission(operatorId, Perms.BUG_TRANSITION, defect.getProjectId());

        WfTransition rule = wfTransitionDao.findByWorkflowIdAndFromStateAndCode(
                defect.getWorkflowId(), defect.getStatusCode(), transitionCode);
        if (rule == null) {
            rule = wfTransitionDao.findByWorkflowIdAndFromStateAndCode(
                    defect.getWorkflowId(), WILDCARD_STATE, transitionCode);
        }
        if (rule == null) {
            throw new BizException(Errors.ILLEGAL_TRANSITION);
        }
        if (rule.isRequireComment() && (comment == null || comment.isBlank())) {
            throw new BizException(Errors.TRANSITION_COMMENT_REQUIRED);
        }
        // 流转限定角色(如 verify 限 QA):wf_transition.require_role 此处真正生效
        permissionManager.checkRole(operatorId, rule.getRequireRole(), defect.getProjectId());

        String from = defect.getStatusCode();
        long durationSec = secondsSinceEnteredCurrentState(defect);
        defect.setStatusCode(rule.getToState());
        applyStateSideEffects(defect, rule.getToState());
        defectDao.save(defect);

        writeHistory(defect.getId(), from, rule.getToState(), rule.getCode(), durationSec, comment, operatorId);
        writeActivity(defect.getId(), "TRANSITION", from + " → " + rule.getToState() + " (" + rule.getName() + ")", operatorId);

        log.info("defect#{} {} -> {} by {}", defectId, from, rule.getToState(), operatorId);
        feishuNotifyManager.notifyDefectChanged(defect, rule.getName(), userNameResolver.name(operatorId));
        return defect;
    }

    @Transactional
    public Defect assignAssignee(Long defectId, Long assigneeId, Long operatorId) {
        Defect defect = defectDao.findById(defectId)
                .orElseThrow(() -> new BizException(Errors.DEFECT_NOT_FOUND));
        permissionManager.checkPermission(operatorId, Perms.BUG_UPDATE, defect.getProjectId());
        defect.setAssigneeId(assigneeId);
        defectDao.save(defect);
        writeActivity(defectId, "ASSIGN", "指派负责人 -> " + userNameResolver.name(assigneeId), operatorId);
        if (assigneeId != null && !assigneeId.equals(operatorId)) {
            notificationManager.notify(assigneeId, "ASSIGN",
                    "缺陷 #" + defectId + " 指派给你:" + defect.getTitle(), "DEFECT", defectId);
        }
        return defect;
    }

    @Transactional
    public Defect setFixVersion(Long defectId, Long versionId, Long userId) {
        Defect defect = defectDao.findById(defectId)
                .orElseThrow(() -> new BizException(Errors.DEFECT_NOT_FOUND));
        permissionManager.checkPermission(userId, Perms.BUG_UPDATE, defect.getProjectId());
        defect.setFixVersionId(versionId);
        defectDao.save(defect);
        writeActivity(defectId, "SET_VERSION",
                versionId == null ? "清除修复版本" : "设置修复版本 #" + versionId, userId);
        return defect;
    }

    @Transactional
    public Defect assignIteration(Long defectId, Long iterationId, Long userId) {
        Defect defect = defectDao.findById(defectId)
                .orElseThrow(() -> new BizException(Errors.DEFECT_NOT_FOUND));
        permissionManager.checkPermission(userId, Perms.BUG_UPDATE, defect.getProjectId());
        defect.setIterationId(iterationId);
        defectDao.save(defect);
        writeActivity(defectId, "ASSIGN_ITERATION",
                iterationId == null ? "移出迭代" : "归入迭代 #" + iterationId, userId);
        return defect;
    }

    public Page<Defect> query(ListDefectReq req, Long userId) {
        if (req.getProjectId() != null) {
            permissionManager.checkPermission(userId, Perms.BUG_VIEW, req.getProjectId());
        }
        // 数据范围:未指定项目时,仅返回用户可见项目(SYS_ADMIN 不受限)
        Set<Long> accessible = req.getProjectId() == null ? permissionManager.accessibleProjectIds(userId) : null;

        Specification<Defect> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (req.getProjectId() != null) {
                ps.add(cb.equal(root.get("projectId"), req.getProjectId()));
            } else if (accessible != null) {
                // 非 SYS_ADMIN:限制在可见项目内;无可见项目则查不到任何数据
                if (accessible.isEmpty()) {
                    ps.add(cb.disjunction());
                } else {
                    ps.add(root.get("projectId").in(accessible));
                }
            }
            if (req.getStatusCode() != null && !req.getStatusCode().isBlank()) {
                ps.add(cb.equal(root.get("statusCode"), req.getStatusCode()));
            }
            if (req.getAssigneeId() != null) {
                ps.add(cb.equal(root.get("assigneeId"), req.getAssigneeId()));
            }
            if (req.getPriority() != null && !req.getPriority().isBlank()) {
                ps.add(cb.equal(root.get("priority"), Priority.valueOf(req.getPriority())));
            }
            if (req.getKeyword() != null && !req.getKeyword().isBlank()) {
                ps.add(cb.like(root.get("title"), "%" + req.getKeyword() + "%"));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        Pageable pageable = PageRequest.of(
                Math.max(0, req.getPn() - 1), req.getPs(), Sort.by(Sort.Order.desc("createTime")));
        return defectDao.findAll(spec, pageable);
    }

    public DefectDetailDto detail(Long defectId, Long userId) {
        Defect defect = defectDao.findById(defectId)
                .orElseThrow(() -> new BizException(Errors.DEFECT_NOT_FOUND));
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, defect.getProjectId());

        Map<String, String> codeToName = wfTransitionDao.findByWorkflowId(defect.getWorkflowId())
                .stream().collect(Collectors.toMap(WfTransition::getCode, WfTransition::getName, (a, b) -> a));

        List<StatusHistoryDto> history = statusHistoryDao
                .findByEntityTypeAndEntityIdOrderByCreateTimeAsc(ENTITY_DEFECT, defectId)
                .stream().map(h -> {
                    StatusHistoryDto dto = new StatusHistoryDto();
                    dto.setFromState(h.getFromState());
                    dto.setToState(h.getToState());
                    dto.setTransitionName(codeToName.getOrDefault(h.getTransitionCode(), h.getTransitionCode()));
                    dto.setOperatorName(userNameResolver.name(h.getOperatorId()));
                    dto.setComment(h.getComment());
                    dto.setDurationSec(h.getDurationSec());
                    dto.setCreateTime(toEpochMilli(h.getCreateTime()));
                    return dto;
                }).toList();

        DefectDetailDto dto = new DefectDetailDto();
        dto.setDefect(toDto(defect));
        dto.setHistory(history);
        dto.setComments(commentManager.listComments(defectId));
        dto.setAvailableTransitions(availableTransitions(defect));
        return dto;
    }

    /** 当前缺陷在当前状态下可执行的流转(精确态 + 通配态) */
    public List<AvailableTransitionDto> availableTransitions(Defect defect) {
        return wfTransitionDao.findByWorkflowId(defect.getWorkflowId()).stream()
                .filter(t -> defect.getStatusCode().equals(t.getFromState()) || WILDCARD_STATE.equals(t.getFromState()))
                .map(t -> {
                    AvailableTransitionDto a = new AvailableTransitionDto();
                    a.setCode(t.getCode());
                    a.setName(t.getName());
                    a.setRequireComment(t.isRequireComment());
                    return a;
                }).toList();
    }

    public List<BoardColumnDto> board(Long projectId, Long userId) {
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, projectId);
        Map<String, List<Defect>> byStatus = defectDao.findByProjectId(projectId).stream()
                .collect(Collectors.groupingBy(Defect::getStatusCode));
        List<WfState> states = wfStateDao.findByWorkflowIdOrderBySortOrderAsc(DEFAULT_WORKFLOW);
        List<BoardColumnDto> columns = new ArrayList<>();
        for (WfState s : states) {
            List<Defect> defects = byStatus.getOrDefault(s.getCode(), List.of());
            BoardColumnDto col = new BoardColumnDto();
            col.setStatusCode(s.getCode());
            col.setStatusName(s.getName());
            col.setCategory(s.getCategory());
            col.setCount(defects.size());
            col.setDefects(defects.stream().map(this::toDto).toList());
            columns.add(col);
        }
        return columns;
    }

    @Transactional
    public Defect setCustomFields(Long defectId, Map<String, Object> fields, Long userId) {
        Defect defect = defectDao.findById(defectId)
                .orElseThrow(() -> new BizException(Errors.DEFECT_NOT_FOUND));
        permissionManager.checkPermission(userId, Perms.BUG_UPDATE, defect.getProjectId());
        Map<String, Object> merged = parseFields(defect.getCustomFields());
        merged.putAll(fields);
        try {
            defect.setCustomFields(objectMapper.writeValueAsString(merged));
        } catch (Exception e) {
            throw new BizException(Errors.PARAM_INVALID.getCode(), "自定义字段序列化失败");
        }
        defectDao.save(defect);
        writeActivity(defectId, "SET_FIELDS", "更新自定义字段: " + fields.keySet(), userId);
        return defect;
    }

    /** 全文搜索:标题或描述 LIKE(本地 MySQL/H2;生产可换 SearchService→ES) */
    public List<Defect> search(String keyword, Long projectId, Long userId) {
        if (projectId != null) {
            permissionManager.checkPermission(userId, Perms.BUG_VIEW, projectId);
        }
        String kw = keyword == null ? "" : keyword.trim();
        Specification<Defect> spec = (root, q, cb) -> {
            List<Predicate> ps = new ArrayList<>();
            if (projectId != null) {
                ps.add(cb.equal(root.get("projectId"), projectId));
            }
            if (!kw.isEmpty()) {
                ps.add(cb.or(
                        cb.like(root.get("title"), "%" + kw + "%"),
                        cb.like(root.get("description"), "%" + kw + "%")));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return defectDao.findAll(spec, PageRequest.of(0, 50, Sort.by(Sort.Order.desc("createTime")))).getContent();
    }

    /** 缺陷查重:在项目内按标题分词重合度找相似缺陷(轻量,本地可跑;生产可换向量检索) */
    public List<Defect> findDuplicates(String title, Long projectId, Long userId) {
        if (projectId != null) {
            permissionManager.checkPermission(userId, Perms.BUG_VIEW, projectId);
        }
        Set<String> kw = tokenize(title);
        if (kw.isEmpty()) {
            return List.of();
        }
        List<Defect> pool = projectId != null ? defectDao.findByProjectId(projectId) : List.of();
        record Scored(Defect d, long overlap) {
        }
        return pool.stream()
                .map(d -> {
                    Set<String> t = tokenize(d.getTitle());
                    t.retainAll(kw);
                    return new Scored(d, t.size());
                })
                .filter(s -> s.overlap() > 0)
                .sorted((a, b) -> Long.compare(b.overlap(), a.overlap()))
                .limit(10)
                .map(Scored::d)
                .toList();
    }

    /** 2-gram 分词(中英文通用):去标点后取相邻 2 字组合,用重合度衡量相似 */
    private Set<String> tokenize(String s) {
        Set<String> set = new java.util.HashSet<>();
        if (s == null) {
            return set;
        }
        String cleaned = s.toLowerCase().replaceAll("[\\s\\p{Punct}，。；：、（）【】！？]", "");
        for (int i = 0; i + 2 <= cleaned.length(); i++) {
            set.add(cleaned.substring(i, i + 2));
        }
        return set;
    }

    public List<ActivityDto> activity(Long defectId, Long userId) {
        Defect defect = defectDao.findById(defectId)
                .orElseThrow(() -> new BizException(Errors.DEFECT_NOT_FOUND));
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, defect.getProjectId());
        return activityLogDao.findByEntityTypeAndEntityIdOrderByCreateTimeDesc(ENTITY_DEFECT, defectId)
                .stream().map(a -> {
                    ActivityDto dto = new ActivityDto();
                    dto.setAction(a.getAction());
                    dto.setDetail(a.getDetail());
                    dto.setOperatorName(userNameResolver.name(a.getOperatorId()));
                    dto.setCreateTime(toEpochMilli(a.getCreateTime()));
                    return dto;
                }).toList();
    }

    /** 批量流转:逐个独立执行(校验先于落库,失败的不影响其它),返回每条结果 */
    public List<BatchResultDto> batchTransition(List<Long> defectIds, String transitionCode, String comment, Long userId) {
        List<BatchResultDto> results = new ArrayList<>();
        for (Long id : defectIds) {
            try {
                transition(id, transitionCode, comment, userId);
                results.add(BatchResultDto.of(id, true, "ok"));
            } catch (BizException e) {
                results.add(BatchResultDto.of(id, false, e.getMessage()));
            }
        }
        return results;
    }

    public StatsDto stats(Long projectId, Long userId) {
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, projectId);
        List<Defect> all = defectDao.findByProjectId(projectId);
        StatsDto dto = new StatsDto();
        dto.setTotal(all.size());
        dto.setByStatus(countBy(all, Defect::getStatusCode));
        dto.setByPriority(countBy(all, d -> d.getPriority() == null ? "UNSET" : d.getPriority().name()));
        dto.setBySeverity(countBy(all, d -> d.getSeverity() == null ? "UNSET" : d.getSeverity().name()));
        return dto;
    }

    private Map<String, Long> countBy(List<Defect> defects, Function<Defect, String> key) {
        Map<String, Long> m = new LinkedHashMap<>();
        for (Defect d : defects) {
            m.merge(key.apply(d), 1L, Long::sum);
        }
        return m;
    }

    private long secondsSinceEnteredCurrentState(Defect defect) {
        StatusHistory last = statusHistoryDao
                .findFirstByEntityTypeAndEntityIdOrderByCreateTimeDesc(ENTITY_DEFECT, defect.getId());
        LocalDateTime enteredAt = last != null ? last.getCreateTime() : defect.getCreateTime();
        if (enteredAt == null) {
            return 0;
        }
        return Math.max(0, Duration.between(enteredAt, LocalDateTime.now()).getSeconds());
    }

    private void writeHistory(Long defectId, String from, String to, String code, long durationSec, String comment, Long operatorId) {
        StatusHistory h = new StatusHistory();
        h.setEntityType(ENTITY_DEFECT);
        h.setEntityId(defectId);
        h.setFromState(from);
        h.setToState(to);
        h.setTransitionCode(code);
        h.setDurationSec(durationSec);
        h.setComment(comment);
        h.setOperatorId(operatorId);
        statusHistoryDao.save(h);
    }

    private void writeActivity(Long defectId, String action, String detail, Long operatorId) {
        ActivityLog a = new ActivityLog();
        a.setEntityType(ENTITY_DEFECT);
        a.setEntityId(defectId);
        a.setAction(action);
        a.setDetail(detail);
        a.setOperatorId(operatorId);
        activityLogDao.save(a);
    }

    private void applyStateSideEffects(Defect defect, String toState) {
        switch (toState) {
            case "RESOLVED" -> defect.setResolvedAt(LocalDateTime.now());
            case "CLOSED" -> defect.setClosedAt(LocalDateTime.now());
            default -> {
                // no-op
            }
        }
    }

    private int nextSeq(Long projectId) {
        Defect last = defectDao.findFirstByProjectIdOrderBySeqInProjectDesc(projectId);
        return last == null || last.getSeqInProject() == null ? 1 : last.getSeqInProject() + 1;
    }

    public DefectDto toDto(Defect d) {
        DefectDto dto = new DefectDto();
        dto.setId(d.getId());
        dto.setProjectId(d.getProjectId());
        dto.setDisplayKey("P" + d.getProjectId() + "-" + d.getSeqInProject());
        dto.setTitle(d.getTitle());
        dto.setDescription(d.getDescription());
        dto.setStatusCode(d.getStatusCode());
        dto.setSeverity(d.getSeverity() == null ? null : d.getSeverity().name());
        dto.setPriority(d.getPriority() == null ? null : d.getPriority().name());
        dto.setReporterId(d.getReporterId());
        dto.setAssigneeId(d.getAssigneeId());
        dto.setIterationId(d.getIterationId());
        dto.setFixVersionId(d.getFixVersionId());
        dto.setSource(d.getSource() == null ? null : d.getSource().name());
        dto.setCreateTime(toEpochMilli(d.getCreateTime()));
        dto.setUpdateTime(toEpochMilli(d.getUpdateTime()));
        dto.setCustomFields(parseFields(d.getCustomFields()));
        return dto;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseFields(String json) {
        if (json == null || json.isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }

    private Long toEpochMilli(LocalDateTime t) {
        return t == null ? null : t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
