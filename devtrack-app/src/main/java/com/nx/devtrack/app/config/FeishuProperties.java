package com.nx.devtrack.app.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 飞书集成配置。第0期复用团队现成的自定义机器人 Webhook 发通知;
 * SSO 免登/卡片回调待新建飞书企业应用后补 appId/appSecret。
 */
@Data
@ConfigurationProperties(prefix = "devtrack.feishu")
public class FeishuProperties {

    /** 自定义机器人 Webhook 地址;为空则只打日志不真正发送 */
    private String webhookUrl = "";

    /** 缺陷详情页基础地址,用于通知卡片跳转按钮 */
    private String defectUrlBase = "http://localhost:5173/defect/";

    /** 企业应用(SSO/卡片用),第0期可留空(留空则飞书登录走 dev 模式) */
    private String appId = "";
    private String appSecret = "";

    /** 卡片回调签名校验用的 Encrypt Key;留空则跳过校验(本地/dev) */
    private String encryptKey = "";

    /** 扫码授权回调地址(前端页面),拼 authorize URL 用 */
    private String redirectUri = "http://localhost:5173/feishu/callback";
}
