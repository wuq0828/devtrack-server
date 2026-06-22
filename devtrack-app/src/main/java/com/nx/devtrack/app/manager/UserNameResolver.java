package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.UserDao;
import com.nx.devtrack.app.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * userId -> 展示名,供历史/评论/审计回显。优先 displayName,退化到 username,再退化到 id。
 */
@Component
@RequiredArgsConstructor
public class UserNameResolver {

    private final UserDao userDao;

    public String name(Long userId) {
        if (userId == null) {
            return "系统";
        }
        User u = userDao.findById(userId).orElse(null);
        if (u == null) {
            return "用户#" + userId;
        }
        if (u.getDisplayName() != null && !u.getDisplayName().isBlank()) {
            return u.getDisplayName();
        }
        return u.getUsername() != null ? u.getUsername() : "用户#" + userId;
    }
}
