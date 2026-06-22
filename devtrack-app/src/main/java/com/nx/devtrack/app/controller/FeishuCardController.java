package com.nx.devtrack.app.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nx.devtrack.app.config.FeishuProperties;
import com.nx.devtrack.app.manager.FeishuCardManager;
import com.nx.devtrack.app.security.FeishuSignatureVerifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 飞书卡片回调入口(无需登录态,飞书直接调用)。
 *  - 配置了 encryptKey 时校验签名(生产);留空则跳过(本地/dev,便于 curl 模拟)。
 *  - URL 验证:回显 challenge。
 *  - 卡片按钮点击:解析 action.value + 点击人 open_id,执行操作,返回 toast。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/devtrack/feishu")
public class FeishuCardController {

    private final FeishuCardManager feishuCardManager;
    private final FeishuSignatureVerifier signatureVerifier;
    private final FeishuProperties feishuProperties;
    private final ObjectMapper objectMapper;

    @PostMapping("/card-callback")
    @SuppressWarnings("unchecked")
    public Object cardCallback(@RequestBody(required = false) String rawBody,
                               @RequestHeader(value = "X-Lark-Request-Timestamp", required = false) String timestamp,
                               @RequestHeader(value = "X-Lark-Request-Nonce", required = false) String nonce,
                               @RequestHeader(value = "X-Lark-Signature", required = false) String signature) throws Exception {
        if (rawBody == null || rawBody.isBlank()) {
            return Map.of("error", "empty body");
        }
        // 签名校验(仅当配置了 encryptKey)
        String key = feishuProperties.getEncryptKey();
        if (key != null && !key.isBlank()
                && !signatureVerifier.verify(timestamp, nonce, key, rawBody, signature)) {
            log.warn("[FeishuCard] 签名校验失败");
            return Map.of("error", "invalid signature");
        }

        Map<String, Object> body = objectMapper.readValue(rawBody, Map.class);

        // URL 验证
        if ("url_verification".equals(body.get("type"))) {
            return Map.of("challenge", body.get("challenge"));
        }

        // 解析 open_id 与 action.value(兼容官方 v2 嵌套与扁平结构)
        String openId = null;
        Map<String, Object> value = null;

        Object eventObj = body.get("event");
        if (eventObj instanceof Map<?, ?> event) {
            Object operator = ((Map<String, Object>) event).get("operator");
            if (operator instanceof Map<?, ?> op) {
                openId = (String) ((Map<String, Object>) op).get("open_id");
            }
            Object action = ((Map<String, Object>) event).get("action");
            if (action instanceof Map<?, ?> act) {
                value = (Map<String, Object>) ((Map<String, Object>) act).get("value");
            }
        }
        if (openId == null) {
            openId = (String) body.get("open_id");
        }
        if (value == null) {
            Object action = body.get("action");
            if (action instanceof Map<?, ?> act) {
                Object v = ((Map<String, Object>) act).get("value");
                value = v instanceof Map ? (Map<String, Object>) v : (Map<String, Object>) action;
            }
        }

        String message = feishuCardManager.handleAction(value, openId);
        return Map.of("toast", Map.of("type", "info", "content", message));
    }
}
