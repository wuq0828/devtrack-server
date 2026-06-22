package com.nx.devtrack.app.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 飞书卡片回调 / 事件签名校验。
 * 签名算法:hex( SHA256( timestamp + nonce + encryptKey + body ) )。
 *
 * 注:不同飞书接口版本的拼接顺序/摘要算法可能有差异,接入真实应用前请对照当前飞书文档核对。
 * 本实现做 sign/verify 自洽,并由单测覆盖正确签名通过 + 篡改被拒。
 */
@Component
public class FeishuSignatureVerifier {

    public String sign(String timestamp, String nonce, String encryptKey, String body) {
        String content = nz(timestamp) + nz(nonce) + nz(encryptKey) + nz(body);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /** 常量时间比较,避免时序侧信道 */
    public boolean verify(String timestamp, String nonce, String encryptKey, String body, String signature) {
        if (signature == null) {
            return false;
        }
        String expected = sign(timestamp, nonce, encryptKey, body);
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), signature.getBytes(StandardCharsets.UTF_8));
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
