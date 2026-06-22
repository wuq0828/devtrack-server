package com.nx.devtrack.app.storage;

import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.BucketExistsArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * MinIO 对象存储(生产)。私有桶,DB 只存 object_key;读取走鉴权后由后端代取(技术方案 §9.2)。
 * 仅当 devtrack.storage.type=minio 时启用。
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "devtrack.storage.type", havingValue = "minio")
public class MinioStorage implements StorageService {

    private final StorageProperties props;
    private final MinioClient client;

    public MinioStorage(StorageProperties props) {
        this.props = props;
        this.client = MinioClient.builder()
                .endpoint(props.getEndpoint())
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }

    @PostConstruct
    public void init() {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(props.getBucket()).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(props.getBucket()).build());
            }
            log.info("[Storage] MinIO 就绪, bucket={}", props.getBucket());
        } catch (Exception e) {
            log.error("[Storage] MinIO 初始化失败: {}", e.getMessage());
        }
    }

    @Override
    public void store(String objectKey, byte[] data, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(props.getBucket())
                    .object(objectKey)
                    .stream(new ByteArrayInputStream(data), data.length, -1)
                    .contentType(contentType == null ? "application/octet-stream" : contentType)
                    .build());
        } catch (Exception e) {
            log.warn("[Storage] MinIO 写入失败: {}", e.getMessage());
            throw new BizException(Errors.INTERNAL_ERROR);
        }
    }

    @Override
    public byte[] load(String objectKey) {
        try (InputStream is = client.getObject(GetObjectArgs.builder()
                .bucket(props.getBucket()).object(objectKey).build())) {
            return is.readAllBytes();
        } catch (Exception e) {
            throw new BizException(Errors.PARAM_INVALID.getCode(), "附件不存在或读取失败");
        }
    }
}
