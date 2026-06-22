package com.nx.devtrack.app.security;

import com.nx.devtrack.app.dao.RoleDao;
import com.nx.devtrack.app.dao.UserRoleDao;
import com.nx.devtrack.app.model.Role;
import com.nx.devtrack.app.model.UserRole;
import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RBAC 鉴权:功能权限 + 项目数据范围(技术方案 §7)。
 *
 * 双轴:角色 -> 功能权限(能不能点这个按钮);user_role.project_id -> 数据范围(能管哪个项目)。
 * SYS_ADMIN 为全局角色,短路放行所有。
 */
@Service
@RequiredArgsConstructor
public class PermissionManager {

    private final UserRoleDao userRoleDao;
    private final RoleDao roleDao;

    /** 用户在指定项目下生效的角色 code 集合(含全局角色)。projectId 为 null 时取该用户全部角色 */
    public Set<String> roleCodes(Long userId, Long projectId) {
        List<UserRole> urs = userRoleDao.findByUserId(userId);
        List<Long> roleIds = urs.stream()
                .filter(ur -> projectId == null || ur.getProjectId() == null || projectId.equals(ur.getProjectId()))
                .map(UserRole::getRoleId)
                .distinct()
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return Set.of();
        }
        return roleDao.findByIdIn(roleIds).stream().map(Role::getCode).collect(Collectors.toSet());
    }

    public boolean isSysAdmin(Long userId) {
        return roleCodes(userId, null).contains(Perms.ROLE_SYS_ADMIN);
    }

    /**
     * 用户可见的项目 id 集合(数据范围)。
     * 返回 null 表示不受限(SYS_ADMIN 可见全部),调用方据此决定是否加 project_id 过滤。
     */
    public Set<Long> accessibleProjectIds(Long userId) {
        if (isSysAdmin(userId)) {
            return null;
        }
        return userRoleDao.findByUserId(userId).stream()
                .map(UserRole::getProjectId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public boolean hasPermission(Long userId, String perm, Long projectId) {
        Set<String> codes = roleCodes(userId, projectId);
        if (codes.contains(Perms.ROLE_SYS_ADMIN)) {
            return true;
        }
        Set<String> perms = new HashSet<>();
        for (String code : codes) {
            perms.addAll(Perms.ROLE_PERMS.getOrDefault(code, Set.of()));
        }
        return perms.contains(perm);
    }

    public void checkPermission(Long userId, String perm, Long projectId) {
        if (!hasPermission(userId, perm, projectId)) {
            throw new BizException(Errors.NO_PERMISSION);
        }
    }

    /** 校验用户在该项目拥有某角色(SYS_ADMIN 放行);用于 wf_transition.require_role 强约束 */
    public boolean hasRole(Long userId, String roleCode, Long projectId) {
        if (roleCode == null || roleCode.isBlank()) {
            return true;
        }
        Set<String> codes = roleCodes(userId, projectId);
        return codes.contains(Perms.ROLE_SYS_ADMIN) || codes.contains(roleCode);
    }

    public void checkRole(Long userId, String roleCode, Long projectId) {
        if (!hasRole(userId, roleCode, projectId)) {
            throw new BizException(Errors.ROLE_REQUIRED);
        }
    }
}
