package com.nx.devtrack.app.security;

import com.nx.devtrack.app.dao.RoleDao;
import com.nx.devtrack.app.dao.UserRoleDao;
import com.nx.devtrack.app.model.Role;
import com.nx.devtrack.app.model.UserRole;
import com.nx.devtrack.common.exception.BizException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PermissionManagerTest {

    private final UserRoleDao userRoleDao = mock(UserRoleDao.class);
    private final RoleDao roleDao = mock(RoleDao.class);
    private final PermissionManager pm = new PermissionManager(userRoleDao, roleDao);

    private Role role(long id, String code) {
        Role r = new Role();
        r.setId(id);
        r.setCode(code);
        return r;
    }

    private UserRole userRole(long roleId, Long projectId) {
        UserRole ur = new UserRole();
        ur.setRoleId(roleId);
        ur.setProjectId(projectId);
        return ur;
    }

    @Test
    void sysAdmin_hasAllPermissions() {
        when(userRoleDao.findByUserId(1L)).thenReturn(List.of(userRole(10L, null)));
        when(roleDao.findByIdIn(List.of(10L))).thenReturn(List.of(role(10L, "SYS_ADMIN")));

        assertThat(pm.hasPermission(1L, Perms.BUG_CREATE, 1L)).isTrue();
        assertThat(pm.hasPermission(1L, Perms.MIGRATION_RUN, 99L)).isTrue();
        assertThat(pm.accessibleProjectIds(1L)).isNull(); // 不受限
    }

    @Test
    void guest_canViewButNotCreate() {
        when(userRoleDao.findByUserId(2L)).thenReturn(List.of(userRole(20L, 1L)));
        when(roleDao.findByIdIn(List.of(20L))).thenReturn(List.of(role(20L, "GUEST")));

        assertThat(pm.hasPermission(2L, Perms.BUG_VIEW, 1L)).isTrue();
        assertThat(pm.hasPermission(2L, Perms.BUG_CREATE, 1L)).isFalse();
        assertThatThrownBy(() -> pm.checkPermission(2L, Perms.BUG_CREATE, 1L))
                .isInstanceOf(BizException.class);
    }

    @Test
    void projectScopedRole_doesNotLeakToOtherProject() {
        when(userRoleDao.findByUserId(3L)).thenReturn(List.of(userRole(30L, 1L)));
        when(roleDao.findByIdIn(List.of(30L))).thenReturn(List.of(role(30L, "QA")));

        // 项目 1 有 QA 权限
        assertThat(pm.hasPermission(3L, Perms.BUG_CREATE, 1L)).isTrue();
        // 项目 2 无任何角色 -> 无权限
        assertThat(pm.hasPermission(3L, Perms.BUG_CREATE, 2L)).isFalse();
    }
}
