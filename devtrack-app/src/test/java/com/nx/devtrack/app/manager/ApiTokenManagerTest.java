package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.ApiTokenDao;
import com.nx.devtrack.app.model.ApiToken;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ApiTokenManagerTest {

    private final ApiTokenDao apiTokenDao = mock(ApiTokenDao.class);
    private final ApiTokenManager manager = new ApiTokenManager(apiTokenDao);

    @Test
    void sha256Hex_matchesKnownVector() {
        // SHA-256("abc") 标准向量
        assertThat(ApiTokenManager.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(ApiTokenManager.sha256Hex("anything")).hasSize(64);
    }

    @Test
    void verify_matchesStoredHash() {
        String token = "dvt_live_demo_token";
        when(apiTokenDao.findByTokenHash(ApiTokenManager.sha256Hex(token))).thenReturn(new ApiToken());

        assertThat(manager.verify(token)).isTrue();
        assertThat(manager.verify("wrong-token")).isFalse();
        assertThat(manager.verify(null)).isFalse();
        assertThat(manager.verify("")).isFalse();
    }
}
