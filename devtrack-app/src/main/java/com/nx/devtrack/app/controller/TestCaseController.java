package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.manager.TestCaseManager;
import com.nx.devtrack.app.util.UserContext;
import com.nx.devtrack.common.dto.TestCaseDto;
import com.nx.devtrack.common.request.CreateTestCaseReq;
import com.nx.devtrack.common.request.DeleteTestCaseReq;
import com.nx.devtrack.common.request.ProjectScopeReq;
import com.nx.devtrack.common.request.SetTestCaseRegressionReq;
import com.nx.devtrack.common.request.UpdateTestCaseReq;
import com.nx.devtrack.common.request.UpdateTestCaseStatusReq;
import com.nx.devtrack.common.web.CommonResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/devtrack/testcase")
public class TestCaseController {

    private final TestCaseManager testCaseManager;

    @PostMapping("/create")
    public CommonResponse<TestCaseDto> create(@Valid @RequestBody CreateTestCaseReq req) {
        return CommonResponse.ok(testCaseManager.toDto(
                testCaseManager.create(req, UserContext.getCurrentUserId())));
    }

    @PostMapping("/list")
    public CommonResponse<List<TestCaseDto>> list(@Valid @RequestBody ProjectScopeReq req) {
        List<TestCaseDto> list = testCaseManager.list(req.getProjectId(), UserContext.getCurrentUserId())
                .stream().map(testCaseManager::toDto).toList();
        return CommonResponse.ok(list);
    }

    @PostMapping("/update-status")
    public CommonResponse<TestCaseDto> updateStatus(@Valid @RequestBody UpdateTestCaseStatusReq req) {
        return CommonResponse.ok(testCaseManager.toDto(
                testCaseManager.updateStatus(req.getTestCaseId(), req.getStatus(), UserContext.getCurrentUserId())));
    }

    @PostMapping("/update")
    public CommonResponse<TestCaseDto> update(@Valid @RequestBody UpdateTestCaseReq req) {
        return CommonResponse.ok(testCaseManager.toDto(
                testCaseManager.update(req, UserContext.getCurrentUserId())));
    }

    @PostMapping("/set-regression")
    public CommonResponse<TestCaseDto> setRegression(@Valid @RequestBody SetTestCaseRegressionReq req) {
        return CommonResponse.ok(testCaseManager.toDto(
                testCaseManager.setRegression(req.getTestCaseId(), req.getRegression(), UserContext.getCurrentUserId())));
    }

    @PostMapping("/delete")
    public CommonResponse<Void> delete(@Valid @RequestBody DeleteTestCaseReq req) {
        testCaseManager.delete(req.getTestCaseId(), UserContext.getCurrentUserId());
        return CommonResponse.ok();
    }
}
