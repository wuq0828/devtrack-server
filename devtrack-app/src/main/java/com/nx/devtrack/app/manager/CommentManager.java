package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.ActivityLogDao;
import com.nx.devtrack.app.dao.CommentDao;
import com.nx.devtrack.app.dao.DefectDao;
import com.nx.devtrack.app.dao.UserDao;
import com.nx.devtrack.app.model.ActivityLog;
import com.nx.devtrack.app.model.Comment;
import com.nx.devtrack.app.model.Defect;
import com.nx.devtrack.app.model.User;
import com.nx.devtrack.app.security.PermissionManager;
import com.nx.devtrack.app.security.Perms;
import com.nx.devtrack.common.dto.CommentItemDto;
import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 评论域。评论挂在缺陷(entityType=DEFECT)上;有 BUG:VIEW 权限即可评论。
 */
@Service
@RequiredArgsConstructor
public class CommentManager {

    private static final String ENTITY_DEFECT = "DEFECT";
    private static final Pattern MENTION = Pattern.compile("@([A-Za-z0-9_]+)");

    private final CommentDao commentDao;
    private final DefectDao defectDao;
    private final ActivityLogDao activityLogDao;
    private final UserDao userDao;
    private final PermissionManager permissionManager;
    private final UserNameResolver userNameResolver;
    private final NotificationManager notificationManager;

    @Transactional
    public CommentItemDto addComment(Long defectId, String content, Long authorId) {
        Defect defect = defectDao.findById(defectId)
                .orElseThrow(() -> new BizException(Errors.DEFECT_NOT_FOUND));
        permissionManager.checkPermission(authorId, Perms.BUG_VIEW, defect.getProjectId());

        Comment c = new Comment();
        c.setEntityType(ENTITY_DEFECT);
        c.setEntityId(defectId);
        c.setContent(content);
        c.setAuthorId(authorId);
        commentDao.save(c);

        ActivityLog log = new ActivityLog();
        log.setEntityType(ENTITY_DEFECT);
        log.setEntityId(defectId);
        log.setAction("COMMENT");
        log.setDetail(content.length() > 200 ? content.substring(0, 200) : content);
        log.setOperatorId(authorId);
        activityLogDao.save(log);

        notifyMentions(content, defectId, authorId);
        return toDto(c);
    }

    /** 解析评论里的 @username,给被提及的用户发 MENTION 通知(去重、排除自己) */
    private void notifyMentions(String content, Long defectId, Long authorId) {
        Matcher m = MENTION.matcher(content == null ? "" : content);
        Set<String> seen = new HashSet<>();
        while (m.find()) {
            String username = m.group(1);
            if (!seen.add(username)) {
                continue;
            }
            User u = userDao.findByUsername(username);
            if (u != null && !u.getId().equals(authorId)) {
                notificationManager.notify(u.getId(), "MENTION",
                        userNameResolver.name(authorId) + " 在缺陷 #" + defectId + " 评论中@了你", "DEFECT", defectId);
            }
        }
    }

    public List<CommentItemDto> listComments(Long defectId) {
        return commentDao.findByEntityTypeAndEntityIdOrderByCreateTimeAsc(ENTITY_DEFECT, defectId)
                .stream().map(this::toDto).toList();
    }

    private CommentItemDto toDto(Comment c) {
        CommentItemDto dto = new CommentItemDto();
        dto.setId(c.getId());
        dto.setContent(c.getContent());
        dto.setAuthorName(userNameResolver.name(c.getAuthorId()));
        dto.setCreateTime(c.getCreateTime() == null ? null
                : c.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return dto;
    }
}
