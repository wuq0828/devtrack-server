package com.nx.devtrack.app.storage;

import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 本地文件系统存储(默认,零依赖)。生产切 MinioStorage。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "devtrack.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorage implements StorageService {

    private final StorageProperties props;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(props.getLocalDir()));
            log.info("[Storage] 本地附件目录: {}", props.getLocalDir());
        } catch (Exception e) {
            throw new IllegalStateException("无法创建附件目录: " + props.getLocalDir(), e);
        }
    }

    @Override
    public void store(String objectKey, byte[] data, String contentType) {
        try {
            Path target = Paths.get(props.getLocalDir(), objectKey);
            Files.createDirectories(target.getParent());
            Files.write(target, data);
        } catch (Exception e) {
            log.warn("[Storage] 写入失败: {}", e.getMessage());
            throw new BizException(Errors.INTERNAL_ERROR);
        }
    }

    @Override
    public byte[] load(String objectKey) {
        try {
            return Files.readAllBytes(Paths.get(props.getLocalDir(), objectKey));
        } catch (Exception e) {
            throw new BizException(Errors.PARAM_INVALID.getCode(), "附件不存在或读取失败");
        }
    }
}
