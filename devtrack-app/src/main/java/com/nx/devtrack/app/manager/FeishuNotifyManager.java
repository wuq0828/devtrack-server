package com.nx.devtrack.app.manager;

import com.nx.devtrack.app.config.FeishuProperties;
import com.nx.devtrack.app.model.Defect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * 飞书通知,第0期复用团队现成的自定义机器人 Webhook(对齐 nx-skyline 的 ReportManager.sendReport)。
 *
 * 设计要点(防刷屏是集成生死线,详见技术方案 §8.1):
 *  - 发送失败只告警,绝不影响业务主流程(best-effort)。
 *  - 后续接入「聚合 + 订阅开关 + 免打扰时段」,这里先打通最小闭环。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeishuNotifyManager {

    private final FeishuProperties feishuProperties;
    private final RestClient restClient;

    /** 发送纯文本消息 */
    public void sendText(String content) {
        Map<String, Object> body = Map.of(
                "msg_type", "text",
                "content", Map.of("text", content)
        );
        post(body);
    }

    /** 缺陷状态变化时推送富文本卡片(简化版 post 富文本) */
    public void notifyDefectChanged(Defect defect, String action, String operator) {
        String url = feishuProperties.getDefectUrlBase() + defect.getId();
        String title = String.format("【DevTrack】缺陷 #%d %s", defect.getId(), action);
        // post 富文本:标题 + 若干行
        Map<String, Object> content = Map.of(
                "post", Map.of("zh_cn", Map.of(
                        "title", title,
                        "content", List.of(
                                List.of(
                                        Map.of("tag", "text", "text", "标题:" + defect.getTitle()),
                                        Map.of("tag", "text", "text", "  状态:" + defect.getStatusCode()),
                                        Map.of("tag", "text", "text", "  优先级:" + defect.getPriority())
                                ),
                                List.of(
                                        Map.of("tag", "text", "text", "操作人:" + operator)
                                ),
                                List.of(
                                        Map.of("tag", "a", "text", "点击查看缺陷", "href", url)
                                )
                        )
                ))
        );
        post(Map.of("msg_type", "post", "content", content));
    }

    /**
     * 发送可交互卡片(带「我接单 / 标记已解决」按钮)。
     * 按钮 value 携带 {action, defectId},用户点击后飞书回调 /devtrack/feishu/card-callback。
     * 注:按钮回调需在飞书应用侧配置卡片回调地址;webhook 仅负责展示。
     */
    public void notifyDefectWithActions(Defect defect) {
        post(buildActionCard(defect));
    }

    public Map<String, Object> buildActionCard(Defect defect) {
        String idStr = String.valueOf(defect.getId());
        Map<String, Object> claim = Map.of(
                "tag", "button",
                "text", Map.of("tag", "plain_text", "content", "我接单"),
                "type", "primary",
                "value", Map.of("action", "claim", "defectId", idStr));
        Map<String, Object> resolve = Map.of(
                "tag", "button",
                "text", Map.of("tag", "plain_text", "content", "标记已解决"),
                "type", "default",
                "value", Map.of("action", "resolve", "defectId", idStr));
        Map<String, Object> card = Map.of(
                "config", Map.of("wide_screen_mode", true),
                "header", Map.of(
                        "title", Map.of("tag", "plain_text", "content", "【DevTrack】缺陷 #" + idStr),
                        "template", "red"),
                "elements", List.of(
                        Map.of("tag", "div", "text", Map.of("tag", "lark_md",
                                "content", "**标题**: " + defect.getTitle()
                                        + "\n**状态**: " + defect.getStatusCode()
                                        + "\n**优先级**: " + defect.getPriority())),
                        Map.of("tag", "action", "actions", List.of(claim, resolve))));
        return Map.of("msg_type", "interactive", "card", card);
    }

    private void post(Map<String, Object> body) {
        String webhook = feishuProperties.getWebhookUrl();
        if (webhook == null || webhook.isBlank()) {
            log.info("[Feishu] webhook 未配置,跳过真实发送。body={}", body);
            return;
        }
        try {
            String resp = restClient.post()
                    .uri(webhook)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            log.info("[Feishu] 通知已发送,resp={}", resp);
        } catch (Exception e) {
            // best-effort:通知失败绝不影响业务主流程
            log.warn("[Feishu] 通知发送失败(已忽略): {}", e.getMessage());
        }
    }
}
