package com.nx.devtrack.app.util;

import com.nx.devtrack.app.model.User;

/**
 * 当前登录用户上下文,对齐 nx-skyline 的 UserContext(ThreadLocal)。
 * 由 AuthTokenFilter 在请求进入时 set、请求结束时 clear。
 */
public final class UserContext {

    private static final ThreadLocal<User> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setCurrentUser(User user) {
        CURRENT_USER.set(user);
    }

    public static User getCurrentUser() {
        return CURRENT_USER.get();
    }

    public static Long getCurrentUserId() {
        User user = CURRENT_USER.get();
        return user == null ? null : user.getId();
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
