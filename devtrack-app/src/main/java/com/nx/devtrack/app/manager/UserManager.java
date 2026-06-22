package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.dao.UserDao;
import com.nx.devtrack.app.model.User;
import com.nx.devtrack.app.security.TokenStore;
import com.nx.devtrack.common.dto.LoginDto;
import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 用户域业务,对齐 nx-skyline 的 UserManager:登录刷新 loginToken + lastLoginTime。
 * 密码 BCrypt 校验;登录态写 TokenStore(本地内存 / 生产 Redis)。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserManager {

    /** token 有效期(秒),48h,对齐 AuthTokenFilter */
    private static final long TOKEN_TTL_SECONDS = 48 * 3600L;

    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final TokenStore tokenStore;

    @Transactional
    public LoginDto login(String username, String password) {
        User user = userDao.findByUsername(username);
        if (user == null || user.getPassword() == null
                || !passwordEncoder.matches(password, user.getPassword())) {
            throw new BizException(Errors.PASSWORD_ERROR);
        }
        return issueLogin(user);
    }

    /** 签发登录令牌(刷新 loginToken + lastLoginTime + 写缓存),登录方式无关,飞书 SSO 也复用 */
    @Transactional
    public LoginDto issueLogin(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        user.setLoginToken(token);
        user.setLastLoginTime(LocalDateTime.now());
        userDao.save(user);
        tokenStore.save(token, user.getId(), TOKEN_TTL_SECONDS);

        LoginDto dto = new LoginDto();
        dto.setUserId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setToken(token);
        dto.setAdmin(user.isAdmin());
        return dto;
    }

    /** 按飞书 open_id 找用户,不存在则自动建号(SSO 首次登录) */
    @Transactional
    public User findOrCreateByFeishu(String openId, String displayName) {
        User user = userDao.findByFeishuOpenId(openId);
        if (user != null) {
            return user;
        }
        user = new User();
        user.setFeishuOpenId(openId);
        user.setUsername("fs_" + openId);
        user.setDisplayName(displayName == null || displayName.isBlank() ? openId : displayName);
        user.setAdmin(false);
        return userDao.save(user);
    }

    public User getById(Long id) {
        return userDao.findById(id).orElse(null);
    }
}
