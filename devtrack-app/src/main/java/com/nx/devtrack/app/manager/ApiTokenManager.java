package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.ApiTokenDao;
import com.nx.devtrack.app.model.ApiToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

/**
 * 开放 API 机器令牌管理。明文仅创建时返回一次,库里只存 SHA-256(技术方案 §7.4)。
 */
@Service
@RequiredArgsConstructor
public class ApiTokenManager {

    private final ApiTokenDao apiTokenDao;

    /** 生成新令牌,返回明文(仅此一次);DB 存其 SHA-256 */
    @Transactional
    public String create(String name, Long userId) {
        String plain = "dvt_live_" + UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        ApiToken t = new ApiToken();
        t.setName(name);
        t.setUserId(userId);
        t.setTokenHash(sha256Hex(plain));
        apiTokenDao.save(t);
        return plain;
    }

    /** 幂等播种一个已知明文的令牌(演示用,保证旧 pytest 集成不断) */
    @Transactional
    public void ensureToken(String plain, String name, Long userId) {
        String hash = sha256Hex(plain);
        if (apiTokenDao.findByTokenHash(hash) != null) {
            return;
        }
        ApiToken t = new ApiToken();
        t.setName(name);
        t.setUserId(userId);
        t.setTokenHash(hash);
        apiTokenDao.save(t);
    }

    public boolean verify(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        return apiTokenDao.findByTokenHash(sha256Hex(token)) != null;
    }

    public static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
