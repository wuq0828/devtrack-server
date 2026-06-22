package com.nx.devtrack.app.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FeishuSignatureVerifierTest {

    private final FeishuSignatureVerifier verifier = new FeishuSignatureVerifier();

    @Test
    void verify_correctSignature_passes() {
        String ts = "1718000000";
        String nonce = "abc123";
        String key = "myEncryptKey";
        String body = "{\"type\":\"event\",\"x\":1}";
        String sig = verifier.sign(ts, nonce, key, body);

        assertThat(verifier.verify(ts, nonce, key, body, sig)).isTrue();
    }

    @Test
    void verify_tamperedBody_fails() {
        String ts = "1718000000";
        String nonce = "abc123";
        String key = "myEncryptKey";
        String sig = verifier.sign(ts, nonce, key, "{\"a\":1}");

        assertThat(verifier.verify(ts, nonce, key, "{\"a\":2}", sig)).isFalse();
    }

    @Test
    void verify_nullSignature_fails() {
        assertThat(verifier.verify("1", "n", "k", "b", null)).isFalse();
    }
}
