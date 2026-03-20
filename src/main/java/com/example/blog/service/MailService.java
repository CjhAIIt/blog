package com.example.blog.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {
    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.mock-mode:true}")
    private boolean mockMode;

    @Value("${app.mail.from:no-reply@blog.local}")
    private String fromAddress;

    public MailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSenderProvider = mailSenderProvider;
    }

    public void sendVerificationCode(String email, String username, String code) {
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mockMode || mailSender == null) {
            log.info("邮箱验证模拟发送 -> user={}, email={}, code={}", username, email, code);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(email);
        message.setSubject("烂柯的博客 - 邮箱验证码");
        message.setText("你好，" + username + "。\n\n你的邮箱验证码是：" + code + "\n验证码 10 分钟内有效。");
        mailSender.send(message);
    }
}
