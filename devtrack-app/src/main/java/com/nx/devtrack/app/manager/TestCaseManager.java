package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.TestCaseDao;
import com.nx.devtrack.app.model.TestCase;
import com.nx.devtrack.app.security.PermissionManager;
import com.nx.devtrack.app.security.Perms;
import com.nx.devtrack.common.dto.TestCaseDto;
import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import com.nx.devtrack.common.request.CreateTestCaseReq;
import com.nx.devtrack.common.request.UpdateTestCaseReq;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TestCaseManager {

    private static final Set<String> VALID_STATUS = Set.of("PENDING", "PASS", "FAIL", "BLOCKED");

    private final TestCaseDao testCaseDao;
    private final PermissionManager permissionManager;

    @Transactional
    public TestCase create(CreateTestCaseReq req, Long userId) {
        permissionManager.checkPermission(userId, Perms.BUG_CREATE, req.getProjectId());
        TestCase tc = new TestCase();
        tc.setProjectId(req.getProjectId());
        tc.setTitle(req.getTitle());
        tc.setPreconditions(req.getPreconditions());
        tc.setSteps(req.getSteps());
        tc.setExpected(req.getExpected());
        tc.setAutomationKey(req.getAutomationKey());
        tc.setStatus("PENDING");
        return testCaseDao.save(tc);
    }

    public List<TestCase> list(Long projectId, Long userId) {
        permissionManager.checkPermission(userId, Perms.BUG_VIEW, projectId);
        return testCaseDao.findByProjectIdOrderByCreateTimeDesc(projectId);
    }

    @Transactional
    public TestCase updateStatus(Long testCaseId, String status, Long userId) {
        if (!VALID_STATUS.contains(status)) {
            throw new BizException(Errors.PARAM_INVALID.getCode(), "非法用例状态: " + status);
        }
        TestCase tc = testCaseDao.findById(testCaseId)
                .orElseThrow(() -> new BizException(Errors.PARAM_INVALID.getCode(), "用例不存在"));
        permissionManager.checkPermission(userId, Perms.BUG_UPDATE, tc.getProjectId());
        tc.setStatus(status);
        return testCaseDao.save(tc);
    }

    @Transactional
    public TestCase setRegression(Long testCaseId, boolean regression, Long userId) {
        TestCase tc = testCaseDao.findById(testCaseId)
                .orElseThrow(() -> new BizException(Errors.PARAM_INVALID.getCode(), "用例不存在"));
        permissionManager.checkPermission(userId, Perms.BUG_UPDATE, tc.getProjectId());
        tc.setRegression(regression);
        return testCaseDao.save(tc);
    }

    @Transactional
    public TestCase update(UpdateTestCaseReq req, Long userId) {
        TestCase tc = testCaseDao.findById(req.getTestCaseId())
                .orElseThrow(() -> new BizException(Errors.PARAM_INVALID.getCode(), "用例不存在"));
        permissionManager.checkPermission(userId, Perms.BUG_UPDATE, tc.getProjectId());
        tc.setTitle(req.getTitle());
        tc.setPreconditions(req.getPreconditions());
        tc.setSteps(req.getSteps());
        tc.setExpected(req.getExpected());
        tc.setAutomationKey(req.getAutomationKey());
        return testCaseDao.save(tc);
    }

    @Transactional
    public void delete(Long testCaseId, Long userId) {
        TestCase tc = testCaseDao.findById(testCaseId)
                .orElseThrow(() -> new BizException(Errors.PARAM_INVALID.getCode(), "用例不存在"));
        permissionManager.checkPermission(userId, Perms.BUG_DELETE, tc.getProjectId());
        // Soft delete: the entity's @SQLRestriction("deleted = false") filters it
        // from all subsequent reads, preserving any test-run references to the row.
        tc.setDeleted(true);
        testCaseDao.save(tc);
    }

    public TestCaseDto toDto(TestCase tc) {
        TestCaseDto dto = new TestCaseDto();
        dto.setId(tc.getId());
        dto.setProjectId(tc.getProjectId());
        dto.setTitle(tc.getTitle());
        dto.setPreconditions(tc.getPreconditions());
        dto.setSteps(tc.getSteps());
        dto.setExpected(tc.getExpected());
        dto.setStatus(tc.getStatus());
        dto.setAutomationKey(tc.getAutomationKey());
        dto.setRegression(tc.isRegression());
        dto.setCreateTime(tc.getCreateTime() == null ? null
                : tc.getCreateTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
        return dto;
    }
}
