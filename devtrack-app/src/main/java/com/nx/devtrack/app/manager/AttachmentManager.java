package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.ActivityLogDao;
import com.nx.devtrack.app.dao.AttachmentDao;
import com.nx.devtrack.app.dao.DefectDao;
import com.nx.devtrack.app.model.ActivityLog;
import com.nx.devtrack.app.model.Attachment;
import com.nx.devtrack.app.model.Defect;
import com.nx.devtrack.app.security.PermissionManager;
import com.nx.devtrack.app.security.Perms;
import com.nx.devtrack.app.storage.StorageService;
import com.nx.devtrack.common.dto.AttachmentDto;
import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttachmentManager {

    private static final String ENTITY_DEFECT = "DEFECT";

    private final AttachmentDao attachmentDao;
    private final DefectDao defectDao;
    private final StorageService storageService;
    private final PermissionManager permissionManager;
    private final UserNameResolver userNameResolver;
    private final ActivityLogDao activityLogDao;

    public record DownloadFile(String fileName, byte[] data) {
    }

    @Transactional
    public AttachmentDto upload(Long defectId, String fileName, byte[] data, String contentType, Long uploaderId) {
        Defect defect = defectDao.findById(defectId)
                .orElseThrow(() -> new BizException(Errors.DEFECT_NOT_FOUND));
        permissionManager.checkPermission(uploaderId, Perms.BUG_UPDATE, defect.getProjectId());

        String objectKey = "defect/" + defectId + "/" + UUID.randomUUID().toString().replace("-", "") + ext(fileName);
        storageService.store(objectKey, data, contentType);

        Attachment a = new Attachment();
        a.setEntityType(ENTITY_DEFECT);
        a.setEntityId(defectId);
        a.setFileName(fileName);
        a.setObjectKey(objectKey);
        a.setSizeBytes((long) data.length);
        a.setUploaderId(uploaderId);
        attachmentDao.save(a);

        ActivityLog log = new ActivityLog();
        log.setEntityType(ENTITY_DEFECT);
        log.setEntityId(defectId);
        log.setAction("ATTACH");
        log.setDetail("上传附件: " + fileName);
        log.setOperatorId(uploaderId);
        activityLogDao.save(log);

        return toDto(a);
    }

    public List<AttachmentDto> list(Long defectId, Long userId) {
        Defect defect = defectDao.findById(defectId)
                .orElseThrow(() -> new BizException(Errors.DEFECT_NOT_FOUND));
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, defect.getProjectId());
        return attachmentDao.findByEntityTypeAndEntityIdOrderByCreateTimeDesc(ENTITY_DEFECT, defectId)
                .stream().map(this::toDto).toList();
    }

    public DownloadFile download(Long attachmentId, Long userId) {
        Attachment a = attachmentDao.findById(attachmentId)
                .orElseThrow(() -> new BizException(Errors.PARAM_INVALID.getCode(), "附件不存在"));
        Defect defect = defectDao.findById(a.getEntityId())
                .orElseThrow(() -> new BizException(Errors.DEFECT_NOT_FOUND));
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, defect.getProjectId());
        return new DownloadFile(a.getFileName(), storageService.load(a.getObjectKey()));
    }

    private String ext(String fileName) {
        if (fileName == null) {
            return "";
        }
        int i = fileName.lastIndexOf('.');
        return i >= 0 ? fileName.substring(i) : "";
    }

    private AttachmentDto toDto(Attachment a) {
        AttachmentDto dto = new AttachmentDto();
        dto.setId(a.getId());
        dto.setFileName(a.getFileName());
        dto.setSizeBytes(a.getSizeBytes());
        dto.setUploaderName(userNameResolver.name(a.getUploaderId()));
        dto.setCreateTime(a.getCreateTime() == null ? null
                : a.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return dto;
    }
}
