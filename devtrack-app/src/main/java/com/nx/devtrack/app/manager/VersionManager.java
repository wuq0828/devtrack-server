package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.VersionDao;
import com.nx.devtrack.app.model.Version;
import com.nx.devtrack.app.security.PermissionManager;
import com.nx.devtrack.app.security.Perms;
import com.nx.devtrack.common.dto.VersionDto;
import com.nx.devtrack.common.request.CreateVersionReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VersionManager {

    private final VersionDao versionDao;
    private final PermissionManager permissionManager;

    @Transactional
    public Version create(CreateVersionReq req, Long userId) {
        permissionManager.checkPermission(userId, Perms.PROJECT_MANAGE, req.getProjectId());
        Version v = new Version();
        v.setProjectId(req.getProjectId());
        v.setName(req.getName());
        v.setStatus("PLANNING");
        return versionDao.save(v);
    }

    public List<Version> list(Long projectId, Long userId) {
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, projectId);
        return versionDao.findByProjectIdOrderByCreateTimeDesc(projectId);
    }

    public VersionDto toDto(Version v) {
        VersionDto dto = new VersionDto();
        dto.setId(v.getId());
        dto.setProjectId(v.getProjectId());
        dto.setName(v.getName());
        dto.setStatus(v.getStatus());
        return dto;
    }
}
