package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.DefectDao;
import com.nx.devtrack.app.dao.EntityTagDao;
import com.nx.devtrack.app.dao.TagDao;
import com.nx.devtrack.app.model.Defect;
import com.nx.devtrack.app.model.EntityTag;
import com.nx.devtrack.app.model.Tag;
import com.nx.devtrack.app.security.PermissionManager;
import com.nx.devtrack.app.security.Perms;
import com.nx.devtrack.common.dto.TagDto;
import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import com.nx.devtrack.common.request.CreateTagReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 多端标签:项目级标签 + 缺陷打标(覆盖式设置)。
 */
@Service
@RequiredArgsConstructor
public class TagManager {

    private static final String ENTITY_DEFECT = "DEFECT";

    private final TagDao tagDao;
    private final EntityTagDao entityTagDao;
    private final DefectDao defectDao;
    private final PermissionManager permissionManager;

    @Transactional
    public TagDto createTag(CreateTagReq req, Long userId) {
        permissionManager.checkPermission(userId, Perms.BUG_UPDATE, req.getProjectId());
        Tag tag = new Tag();
        tag.setProjectId(req.getProjectId());
        tag.setName(req.getName());
        tag.setColor(req.getColor() == null || req.getColor().isBlank() ? "#409EFF" : req.getColor());
        return toDto(tagDao.save(tag));
    }

    public List<TagDto> listTags(Long projectId, Long userId) {
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, projectId);
        return tagDao.findByProjectIdOrderByCreateTimeAsc(projectId).stream().map(this::toDto).toList();
    }

    public List<TagDto> defectTags(Long defectId, Long userId) {
        Defect defect = loadDefect(defectId);
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, defect.getProjectId());
        List<Long> tagIds = entityTagDao.findByEntityTypeAndEntityId(ENTITY_DEFECT, defectId)
                .stream().map(EntityTag::getTagId).toList();
        if (tagIds.isEmpty()) {
            return List.of();
        }
        return tagDao.findByIdIn(tagIds).stream().map(this::toDto).toList();
    }

    @Transactional
    public List<TagDto> setDefectTags(Long defectId, List<Long> tagIds, Long userId) {
        Defect defect = loadDefect(defectId);
        permissionManager.checkPermission(userId, Perms.BUG_UPDATE, defect.getProjectId());
        // 覆盖式:删旧建新
        entityTagDao.deleteAll(entityTagDao.findByEntityTypeAndEntityId(ENTITY_DEFECT, defectId));
        for (Long tagId : tagIds) {
            EntityTag et = new EntityTag();
            et.setEntityType(ENTITY_DEFECT);
            et.setEntityId(defectId);
            et.setTagId(tagId);
            entityTagDao.save(et);
        }
        return defectTags(defectId, userId);
    }

    private Defect loadDefect(Long defectId) {
        return defectDao.findById(defectId).orElseThrow(() -> new BizException(Errors.DEFECT_NOT_FOUND));
    }

    private TagDto toDto(Tag t) {
        TagDto dto = new TagDto();
        dto.setId(t.getId());
        dto.setName(t.getName());
        dto.setColor(t.getColor());
        return dto;
    }
}
