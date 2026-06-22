package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.NotificationDao;
import com.nx.devtrack.app.model.Notification;
import com.nx.devtrack.common.dto.NotificationDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;

/**
 * 站内信通知 + 邮件通道。通知落库供前端铃铛拉取,同时通过 EmailSender 旁路发邮件(本地为日志)。
 */
@Service
@RequiredArgsConstructor
public class NotificationManager {

    private final NotificationDao notificationDao;
    private final EmailSender emailSender;
    private final UserNameResolver userNameResolver;

    @Transactional
    public void notify(Long userId, String type, String content, String refType, Long refId) {
        if (userId == null) {
            return;
        }
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setContent(content);
        n.setRefType(refType);
        n.setRefId(refId);
        n.setReadFlag(false);
        notificationDao.save(n);
        // 旁路邮件(best-effort)
        emailSender.send(userNameResolver.name(userId), "[DevTrack] " + type, content);
    }

    public List<NotificationDto> listForUser(Long userId, boolean onlyUnread) {
        List<Notification> list = onlyUnread
                ? notificationDao.findByUserIdAndReadFlagFalseOrderByCreateTimeDesc(userId)
                : notificationDao.findByUserIdOrderByCreateTimeDesc(userId);
        return list.stream().map(this::toDto).toList();
    }

    public long unreadCount(Long userId) {
        return notificationDao.countByUserIdAndReadFlagFalse(userId);
    }

    @Transactional
    public void markRead(Long id, Long userId) {
        notificationDao.findById(id).ifPresent(n -> {
            if (n.getUserId().equals(userId)) {
                n.setReadFlag(true);
                notificationDao.save(n);
            }
        });
    }

    @Transactional
    public void markAllRead(Long userId) {
        List<Notification> unread = notificationDao.findByUserIdAndReadFlagFalseOrderByCreateTimeDesc(userId);
        unread.forEach(n -> n.setReadFlag(true));
        notificationDao.saveAll(unread);
    }

    private NotificationDto toDto(Notification n) {
        NotificationDto dto = new NotificationDto();
        dto.setId(n.getId());
        dto.setType(n.getType());
        dto.setContent(n.getContent());
        dto.setRefType(n.getRefType());
        dto.setRefId(n.getRefId());
        dto.setRead(n.isReadFlag());
        dto.setCreateTime(n.getCreateTime() == null ? null
                : n.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return dto;
    }
}
