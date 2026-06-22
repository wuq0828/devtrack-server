package com.nx.devtrack.app.storage;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LocalFileStorageTest {

    @Test
    void storeAndLoad_roundtrip() {
        StorageProperties props = new StorageProperties();
        props.setLocalDir(System.getProperty("java.io.tmpdir") + "/devtrack-test-"
                + UUID.randomUUID().toString().replace("-", ""));
        LocalFileStorage storage = new LocalFileStorage(props);
        storage.init();

        byte[] data = "hello attachment 内容".getBytes(StandardCharsets.UTF_8);
        storage.store("defect/1/abc.txt", data, "text/plain");

        assertThat(storage.load("defect/1/abc.txt")).isEqualTo(data);
    }
}
