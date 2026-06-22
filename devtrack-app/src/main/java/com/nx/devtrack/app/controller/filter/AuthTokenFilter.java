package com.nx.devtrack.app.controller.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nx.devtrack.app.dao.UserDao;
import com.nx.devtrack.app.model.User;
import com.nx.devtrack.app.security.TokenStore;
import com.nx.devtrack.app.util.UserContext;
import com.nx.devtrack.common.exception.Errors;
import com.nx.devtrack.common.web.CommonResponse;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * 登录鉴权过滤器,对齐 nx-skyline 的自研 Token 机制:
 * Authorization: Bearer {loginToken} -> 先查 TokenStore 缓存,未命中回退 DB(48h 过期)-> 注入 UserContext。
 *
 * 放行:登录接口、自动化集成接口(走 API Token)、飞书回调、健康检查。
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class AuthTokenFilter implements Filter {

    /** token 有效期(小时),对齐 nx-skyline 的 48h */
    private static final long TOKEN_VALID_HOURS = 48;
    private static final long TOKEN_TTL_SECONDS = TOKEN_VALID_HOURS * 3600L;

    private final UserDao userDao;
    private final TokenStore tokenStore;
    private final ObjectMapper objectMapper;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpResp = (HttpServletResponse) response;
        String path = httpReq.getRequestURI();

        // 仅守护业务接口;放行登录 / 自动化集成(API Token) / 飞书回调 / 健康检查
        if (!path.startsWith("/devtrack/")
                || path.startsWith("/devtrack/auth/")
                || path.startsWith("/devtrack/integrations/")
                || path.startsWith("/devtrack/feishu/")
                || path.startsWith("/devtrack/health")) {
            chain.doFilter(request, response);
            return;
        }

        try {
            String authHeader = httpReq.getHeader("Authorization");
            String loginToken = authHeader == null ? null : authHeader.replace("Bearer ", "").trim();
            if (loginToken == null || loginToken.isEmpty()) {
                writeError(httpResp);
                return;
            }
            User user = resolveUser(loginToken);
            if (user == null) {
                writeError(httpResp);
                return;
            }
            UserContext.setCurrentUser(user);
            chain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    /** 先查缓存(命中即取 userId),未命中回退 DB 校验 48h 并回填缓存 */
    private User resolveUser(String loginToken) {
        Long userId = tokenStore.getUserId(loginToken);
        if (userId != null) {
            return userDao.findById(userId).orElse(null);
        }
        User user = userDao.findByLoginToken(loginToken);
        if (user == null || user.getLastLoginTime() == null
                || user.getLastLoginTime().plusHours(TOKEN_VALID_HOURS).isBefore(LocalDateTime.now())) {
            return null;
        }
        // 缓存未命中但 DB 有效(如重启后缓存丢失):回填缓存
        tokenStore.save(loginToken, user.getId(), TOKEN_TTL_SECONDS);
        return user;
    }

    private void writeError(HttpServletResponse resp) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json;charset=UTF-8");
        resp.getWriter().write(objectMapper.writeValueAsString(CommonResponse.fail(Errors.NEED_LOGIN)));
    }
}
