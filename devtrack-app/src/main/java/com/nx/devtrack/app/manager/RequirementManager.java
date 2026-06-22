package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.RequirementDao;
import com.nx.devtrack.app.model.Requirement;
import com.nx.devtrack.app.security.PermissionManager;
import com.nx.devtrack.app.security.Perms;
import com.nx.devtrack.common.dto.RequirementDto;
import com.nx.devtrack.common.enums.Priority;
import com.nx.devtrack.common.request.CreateRequirementReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequirementManager {

    private final RequirementDao requirementDao;
    private final PermissionManager permissionManager;

    @Transactional
    public Requirement create(CreateRequirementReq req, Long userId) {
        permissionManager.checkPermission(userId, Perms.BUG_CREATE, req.getProjectId());
        Requirement r = new Requirement();
        r.setProjectId(req.getProjectId());
        r.setTitle(req.getTitle());
        r.setDescription(req.getDescription());
        r.setPriority(req.getPriority() == null ? Priority.P2 : req.getPriority());
        r.setStatus("PENDING");
        r.setReporterId(userId);
        return requirementDao.save(r);
    }

    public List<Requirement> list(Long projectId, Long userId) {
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, projectId);
        return requirementDao.findByProjectIdOrderByCreateTimeDesc(projectId);
    }

    public RequirementDto toDto(Requirement r) {
        RequirementDto dto = new RequirementDto();
        dto.setId(r.getId());
        dto.setProjectId(r.getProjectId());
        dto.setTitle(r.getTitle());
        dto.setDescription(r.getDescription());
        dto.setStatus(r.getStatus());
        dto.setPriority(r.getPriority() == null ? null : r.getPriority().name());
        dto.setCreateTime(r.getCreateTime() == null ? null
                : r.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return dto;
    }
}
