package com.nx.devtrack.app.storage;

/**
 * 对象存储抽象。附件二进制按 objectKey 存取;DB 只存 objectKey,绝不存公网直链(技术方案 §9.2)。
 */
public interface StorageService {

    void store(String objectKey, byte[] data, String contentType);

    byte[] load(String objectKey);
}
