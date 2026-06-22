package com.nx.devtrack.app.manager;

/**
 * 邮件发送通道。本地用 LoggingEmailSender(只打日志,零依赖);
 * 生产可加 SmtpEmailSender(spring-boot-starter-mail)并按 profile 切换。
 */
public interface EmailSender {

    void send(String to, String subject, String body);
}
