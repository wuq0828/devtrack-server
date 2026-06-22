package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.UserDao;
import com.nx.devtrack.app.model.User;
import com.nx.devtrack.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 处理飞书卡片按钮回调:把「我接单 / 标记已解决」落成真实的缺陷操作,以点击人(飞书 open_id 映射的用户)身份执行。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuCardManager {

    private final UserDao userDao;
    private final DefectManager defectManager;

    /**
     * @param value  按钮 value,形如 {action: "claim"|"resolve", defectId: "1"}
     * @param openId 点击人飞书 open_id
     * @return 给飞书 toast 展示的结果文案
     */
    public String handleAction(Map<String, Object> value, String openId) {
        if (value == null || openId == null) {
            return "回调参数缺失";
        }
        User user = userDao.findByFeishuOpenId(openId);
        if (user == null) {
            return "未识别的飞书用户,请先在 DevTrack 用飞书登录一次";
        }
        Object action = value.get("action");
        Object defectIdObj = value.get("defectId");
        if (action == null || defectIdObj == null) {
            return "缺少 action 或 defectId";
        }
        Long defectId = Long.valueOf(String.valueOf(defectIdObj));
        try {
            return switch (String.valueOf(action)) {
                case "claim" -> {
                    defectManager.assignAssignee(defectId, user.getId(), user.getId());
                    yield "已接单:" + nameOf(user);
                }
                case "resolve" -> {
                    defectManager.transition(defectId, "resolve", "飞书卡片标记已解决", user.getId());
                    yield "已标记解决";
                }
                default -> "未知操作: " + action;
            };
        } catch (BizException e) {
            log.info("[FeishuCard] 操作被拒: {}", e.getMessage());
            return e.getMessage();
        }
    }

    private String nameOf(User u) {
        return u.getDisplayName() != null && !u.getDisplayName().isBlank() ? u.getDisplayName() : u.getUsername();
    }
}
