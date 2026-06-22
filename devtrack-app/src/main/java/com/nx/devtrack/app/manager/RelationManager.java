package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.DefectDao;
import com.nx.devtrack.app.dao.RelationDao;
import com.nx.devtrack.app.dao.RequirementDao;
import com.nx.devtrack.app.dao.TestCaseDao;
import com.nx.devtrack.app.model.Relation;
import com.nx.devtrack.app.security.PermissionManager;
import com.nx.devtrack.app.security.Perms;
import com.nx.devtrack.common.dto.RelationDto;
import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import com.nx.devtrack.common.request.LinkReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 需求-缺陷-用例三向关联。多态(entityType + id),关联是无向的(查询时 source/target 都算)。
 */
@Service
@RequiredArgsConstructor
public class RelationManager {

    private record EntityRef(String title, Long projectId) {
    }

    private final RelationDao relationDao;
    private final DefectDao defectDao;
    private final RequirementDao requirementDao;
    private final TestCaseDao testCaseDao;
    private final PermissionManager permissionManager;

    @Transactional
    public RelationDto link(LinkReq req, Long userId) {
        EntityRef source = resolve(req.getSourceType(), req.getSourceId());
        EntityRef target = resolve(req.getTargetType(), req.getTargetId());
        permissionManager.checkPermission(userId, Perms.BUG_UPDATE, source.projectId());

        Relation existing = relationDao.findBySourceTypeAndSourceIdAndTargetTypeAndTargetIdAndRelationType(
                req.getSourceType(), req.getSourceId(), req.getTargetType(), req.getTargetId(), req.getRelationType());
        if (existing != null) {
            return toDto(existing);
        }
        Relation r = new Relation();
        r.setSourceType(req.getSourceType());
        r.setSourceId(req.getSourceId());
        r.setTargetType(req.getTargetType());
        r.setTargetId(req.getTargetId());
        r.setRelationType(req.getRelationType());
        relationDao.save(r);
        return toDto(r);
    }

    @Transactional
    public void unlink(Long relationId, Long userId) {
        Relation r = relationDao.findById(relationId)
                .orElseThrow(() -> new BizException(Errors.PARAM_INVALID.getCode(), "关联不存在"));
        EntityRef source = resolve(r.getSourceType(), r.getSourceId());
        permissionManager.checkPermission(userId, Perms.BUG_UPDATE, source.projectId());
        relationDao.delete(r);
    }

    public List<RelationDto> listRelated(String entityType, Long entityId, Long userId) {
        EntityRef ref = resolve(entityType, entityId);
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, ref.projectId());
        // 去重合并:作为 source 或 target 的关联
        Map<Long, Relation> merged = new LinkedHashMap<>();
        relationDao.findBySourceTypeAndSourceId(entityType, entityId).forEach(r -> merged.put(r.getId(), r));
        relationDao.findByTargetTypeAndTargetId(entityType, entityId).forEach(r -> merged.put(r.getId(), r));
        List<RelationDto> out = new ArrayList<>();
        for (Relation r : merged.values()) {
            out.add(toDto(r));
        }
        return out;
    }

    private RelationDto toDto(Relation r) {
        RelationDto dto = new RelationDto();
        dto.setId(r.getId());
        dto.setSourceType(r.getSourceType());
        dto.setSourceId(r.getSourceId());
        dto.setSourceTitle(safeTitle(r.getSourceType(), r.getSourceId()));
        dto.setTargetType(r.getTargetType());
        dto.setTargetId(r.getTargetId());
        dto.setTargetTitle(safeTitle(r.getTargetType(), r.getTargetId()));
        dto.setRelationType(r.getRelationType());
        return dto;
    }

    private String safeTitle(String type, Long id) {
        try {
            return resolve(type, id).title();
        } catch (BizException e) {
            return "(已删除)";
        }
    }

    private EntityRef resolve(String type, Long id) {
        return switch (type == null ? "" : type) {
            case "DEFECT" -> defectDao.findById(id).map(d -> new EntityRef(d.getTitle(), d.getProjectId()))
                    .orElseThrow(() -> new BizException(Errors.DEFECT_NOT_FOUND));
            case "REQUIREMENT" -> requirementDao.findById(id).map(r -> new EntityRef(r.getTitle(), r.getProjectId()))
                    .orElseThrow(() -> new BizException(Errors.PARAM_INVALID.getCode(), "需求不存在"));
            case "TEST_CASE" -> testCaseDao.findById(id).map(t -> new EntityRef(t.getTitle(), t.getProjectId()))
                    .orElseThrow(() -> new BizException(Errors.PARAM_INVALID.getCode(), "用例不存在"));
            default -> throw new BizException(Errors.PARAM_INVALID.getCode(), "未知实体类型: " + type);
        };
    }
}
