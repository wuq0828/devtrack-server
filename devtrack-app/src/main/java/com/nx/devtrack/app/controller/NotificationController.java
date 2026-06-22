package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.manager.NotificationManager;
import com.nx.devtrack.app.util.UserContext;
import com.nx.devtrack.common.dto.NotificationDto;
import com.nx.devtrack.common.web.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/devtrack/notification")
public class NotificationController {

    private final NotificationManager notificationManager;

    @PostMapping("/list")
    public CommonResponse<List<NotificationDto>> list(@RequestBody(required = false) Map<String, Object> body) {
        boolean onlyUnread = body != null && Boolean.TRUE.equals(body.get("onlyUnread"));
        return CommonResponse.ok(notificationManager.listForUser(UserContext.getCurrentUserId(), onlyUnread));
    }

    @PostMapping("/unread-count")
    public CommonResponse<Map<String, Long>> unreadCount() {
        return CommonResponse.ok(Map.of("count", notificationManager.unreadCount(UserContext.getCurrentUserId())));
    }

    @PostMapping("/mark-read")
    public CommonResponse<Void> markRead(@RequestBody Map<String, Object> body) {
        notificationManager.markRead(Long.valueOf(String.valueOf(body.get("id"))), UserContext.getCurrentUserId());
        return CommonResponse.ok();
    }

    @PostMapping("/mark-all-read")
    public CommonResponse<Void> markAllRead() {
        notificationManager.markAllRead(UserContext.getCurrentUserId());
        return CommonResponse.ok();
    }
}
