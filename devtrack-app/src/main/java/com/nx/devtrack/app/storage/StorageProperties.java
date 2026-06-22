package com.nx.devtrack.app.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 附件存储配置。type=local(本地文件系统,默认,零依赖)/ minio(生产对象存储)。
 */
@Data
@ConfigurationProperties(prefix = "devtrack.storage")
public class StorageProperties {

    /** local / minio */
    private String type = "local";

    /** 本地存储目录 */
    private String localDir = System.getProperty("java.io.tmpdir") + "/devtrack-uploads";

    private String endpoint = "http://localhost:9000";
    private String accessKey = "";
    private String secretKey = "";
    private String bucket = "devtrack";
}
