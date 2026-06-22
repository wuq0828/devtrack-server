package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.TestCaseDao;
import com.nx.devtrack.app.security.PermissionManager;
import com.nx.devtrack.common.exception.BizException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class TestCaseManagerTest {

    private final TestCaseDao testCaseDao = mock(TestCaseDao.class);
    private final PermissionManager permissionManager = mock(PermissionManager.class);
    private final TestCaseManager manager = new TestCaseManager(testCaseDao, permissionManager);

    @Test
    void updateStatus_invalidStatus_throws() {
        assertThatThrownBy(() -> manager.updateStatus(1L, "NOT_A_STATUS", 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("非法用例状态");
    }
}
