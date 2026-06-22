package com.nx.devtrack.app.controller;

import com.nx.devtrack.app.manager.TapdMigrationManager;
import com.nx.devtrack.app.security.PermissionManager;
import com.nx.devtrack.app.security.Perms;
import com.nx.devtrack.app.util.UserContext;
import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import com.nx.devtrack.common.web.CommonResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * TAPD 数据迁移入口。上传 TAPD 导出的 CSV,导入为 DevTrack 缺陷。
 * 仅 MIGRATION:RUN 权限(项目负责人 / 管理员)可执行。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/devtrack/migration")
public class MigrationController {

    private final TapdMigrationManager tapdMigrationManager;
    private final PermissionManager permissionManager;

    @PostMapping("/tapd")
    public CommonResponse<Map<String, Object>> migrateTapd(@RequestParam("file") MultipartFile file,
                                                           @RequestParam("projectId") Long projectId) {
        permissionManager.checkPermission(UserContext.getCurrentUserId(), Perms.MIGRATION_RUN, projectId);
        if (file == null || file.isEmpty()) {
            throw new BizException(Errors.MIGRATION_FAILED.getCode(), "上传文件为空");
        }
        String csv;
        try {
            csv = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BizException(Errors.MIGRATION_FAILED);
        }
        return CommonResponse.ok(tapdMigrationManager.migrateCsv(csv, projectId));
    }
}
