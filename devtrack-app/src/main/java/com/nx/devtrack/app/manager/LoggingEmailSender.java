package com.nx.devtrack.app.manager;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

/**
 * 默认邮件通道:只打日志,不真正发信(本地/未配 SMTP 时)。
 */
@Slf4j
@Component
@ConditionalOnMissingBean(name = "smtpEmailSender")
public class LoggingEmailSender implements EmailSender {

    @Override
    public void send(String to, String subject, String body) {
        log.info("[Email] (模拟发送) to={} subject={} body={}", to, subject, body);
    }
}
