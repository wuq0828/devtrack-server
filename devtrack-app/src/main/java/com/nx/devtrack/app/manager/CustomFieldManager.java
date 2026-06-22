package com.nx.devtrack.app.manager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nx.devtrack.app.dao.CustomFieldDefDao;
import com.nx.devtrack.app.model.CustomFieldDef;
import com.nx.devtrack.app.security.PermissionManager;
import com.nx.devtrack.app.security.Perms;
import com.nx.devtrack.common.dto.CustomFieldDefDto;
import com.nx.devtrack.common.request.CreateCustomFieldReq;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomFieldManager {

    private final CustomFieldDefDao customFieldDefDao;
    private final PermissionManager permissionManager;
    private final ObjectMapper objectMapper;

    @Transactional
    public CustomFieldDef createDef(CreateCustomFieldReq req, Long userId) {
        permissionManager.checkPermission(userId, Perms.PROJECT_MANAGE, req.getProjectId());
        CustomFieldDef def = new CustomFieldDef();
        def.setProjectId(req.getProjectId());
        def.setFieldKey(req.getFieldKey());
        def.setLabel(req.getLabel());
        def.setFieldType(req.getFieldType() == null ? "TEXT" : req.getFieldType());
        def.setRequired(req.isRequired());
        def.setOptions(writeOptions(req.getOptions()));
        return customFieldDefDao.save(def);
    }

    public List<CustomFieldDefDto> listDefs(Long projectId, Long userId) {
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, projectId);
        return customFieldDefDao.findByProjectIdOrderByCreateTimeAsc(projectId)
                .stream().map(this::toDto).toList();
    }

    private String writeOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public CustomFieldDefDto toDto(CustomFieldDef def) {
        CustomFieldDefDto dto = new CustomFieldDefDto();
        dto.setId(def.getId());
        dto.setFieldKey(def.getFieldKey());
        dto.setLabel(def.getLabel());
        dto.setFieldType(def.getFieldType());
        dto.setRequired(def.isRequired());
        if (def.getOptions() != null && !def.getOptions().isBlank()) {
            try {
                dto.setOptions(objectMapper.readValue(def.getOptions(), List.class));
            } catch (Exception e) {
                dto.setOptions(List.of());
            }
        } else {
            dto.setOptions(List.of());
        }
        return dto;
    }
}
