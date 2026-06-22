package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.config.FeishuProperties;
import com.nx.devtrack.app.model.User;
import com.nx.devtrack.common.dto.LoginDto;
import com.nx.devtrack.common.exception.BizException;
import com.nx.devtrack.common.exception.Errors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * 飞书扫码登录 SSO(技术方案 §8.1)。
 *
 *  - 已配 appId/appSecret -> 走真实 OAuth:code 换 user_access_token,再拉用户信息,按 open_id 建/匹配账号。
 *  - 未配(dev 模式)-> 把传入的 code 直接当作 open_id,便于本地联调登录闭环。
 *
 * 前端拿 code 的方式:飞书工作台免登 H5 SDK tt.requestAccess(),或网页扫码授权回调。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuAuthManager {

    private static final String APP_TOKEN_URL = "https://open.feishu.cn/open-apis/auth/v3/app_access_token/internal";
    private static final String CODE_TOKEN_URL = "https://open.feishu.cn/open-apis/authen/v1/oidc/access_token";
    private static final String USER_INFO_URL = "https://open.feishu.cn/open-apis/authen/v1/user_info";

    private final FeishuProperties feishuProperties;
    private final RestClient restClient;
    private final UserManager userManager;

    public LoginDto loginByCode(String code) {
        if (code == null || code.isBlank()) {
            throw new BizException(Errors.FEISHU_AUTH_FAILED);
        }
        String openId;
        String name;

        if (feishuProperties.getAppId() == null || feishuProperties.getAppId().isBlank()) {
            // dev 模式:code 即 open_id,便于本地验证登录闭环
            log.info("[FeishuAuth] dev 模式(未配 appId),将 code 当作 open_id: {}", code);
            openId = code;
            name = "飞书用户-" + (code.length() > 6 ? code.substring(code.length() - 6) : code);
        } else {
            Map<String, Object> info = realExchange(code);
            openId = (String) info.get("open_id");
            name = (String) info.get("name");
            if (openId == null) {
                throw new BizException(Errors.FEISHU_AUTH_FAILED);
            }
        }

        User user = userManager.findOrCreateByFeishu(openId, name);
        return userManager.issueLogin(user);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> realExchange(String code) {
        try {
            // 1) app_access_token
            Map<String, Object> appResp = restClient.post().uri(APP_TOKEN_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("app_id", feishuProperties.getAppId(), "app_secret", feishuProperties.getAppSecret()))
                    .retrieve().body(Map.class);
            String appAccessToken = appResp == null ? null : (String) appResp.get("app_access_token");

            // 2) code -> user_access_token
            Map<String, Object> tokenResp = restClient.post().uri(CODE_TOKEN_URL)
                    .header("Authorization", "Bearer " + appAccessToken)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("grant_type", "authorization_code", "code", code))
                    .retrieve().body(Map.class);
            Map<String, Object> tokenData = tokenResp == null ? null : (Map<String, Object>) tokenResp.get("data");
            String userAccessToken = tokenData == null ? null : (String) tokenData.get("access_token");

            // 3) user_access_token -> 用户信息
            Map<String, Object> userResp = restClient.get().uri(USER_INFO_URL)
                    .header("Authorization", "Bearer " + userAccessToken)
                    .retrieve().body(Map.class);
            Map<String, Object> userData = userResp == null ? null : (Map<String, Object>) userResp.get("data");
            if (userData == null) {
                throw new BizException(Errors.FEISHU_AUTH_FAILED);
            }
            return userData;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("[FeishuAuth] 真实 OAuth 失败: {}", e.getMessage());
            throw new BizException(Errors.FEISHU_AUTH_FAILED);
        }
    }
}
