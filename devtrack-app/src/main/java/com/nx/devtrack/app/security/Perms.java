package com.nx.devtrack.app.security;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 权限点常量 + 角色→权限映射(脚手架用代码内置;生产可迁到 sys_permission/sys_role_perm 表)。
 * SYS_ADMIN 拥有全部权限(代码层短路,不在此列举)。
 */
public final class Perms {

    public static final String BUG_VIEW = "BUG:VIEW";
    public static final String BUG_CREATE = "BUG:CREATE";
    public static final String BUG_UPDATE = "BUG:UPDATE";
    public static final String BUG_TRANSITION = "BUG:TRANSITION";
    public static final String BUG_DELETE = "BUG:DELETE";
    public static final String PROJECT_MANAGE = "PROJECT:MANAGE";
    public static final String MIGRATION_RUN = "MIGRATION:RUN";

    public static final String ROLE_SYS_ADMIN = "SYS_ADMIN";

    /** 角色 code -> 权限集合 */
    public static final Map<String, Set<String>> ROLE_PERMS = Map.of(
            "PROJECT_OWNER", Set.of(BUG_VIEW, BUG_CREATE, BUG_UPDATE, BUG_TRANSITION, BUG_DELETE, PROJECT_MANAGE, MIGRATION_RUN),
            "QA", Set.of(BUG_VIEW, BUG_CREATE, BUG_UPDATE, BUG_TRANSITION),
            "DEV", Set.of(BUG_VIEW, BUG_UPDATE, BUG_TRANSITION),
            "PM", Set.of(BUG_VIEW, BUG_CREATE),
            "GUEST", Set.of(BUG_VIEW),
            "SERVICE", Set.of(BUG_VIEW, BUG_CREATE, BUG_TRANSITION)
    );

    /** 所有内置角色 code(SYS_ADMIN 为全局角色,单列) */
    public static final List<String> ALL_ROLES = List.of(
            "SYS_ADMIN", "PROJECT_OWNER", "DEV", "QA", "PM", "GUEST", "SERVICE");

    private Perms() {
    }
}
