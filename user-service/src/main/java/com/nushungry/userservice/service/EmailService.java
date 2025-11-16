package com.nushungry.userservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * 邮件服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    /**
     * 发送简单文本邮件
     *
     * @param to 收件人
     * @param subject 主题
     * @param text 内容
     */
    public void sendSimpleEmail(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            mailSender.send(message);
            log.info("简单邮件已发送至: {}", to);
        } catch (Exception e) {
            log.error("发送简单邮件失败: {}", e.getMessage());
            throw new RuntimeException("发送邮件失败", e);
        }
    }

    /**
     * 发送HTML邮件
     *
     * @param to 收件人
     * @param subject 主题
     * @param htmlContent HTML内容
     */
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("HTML邮件已发送至: {}", to);
        } catch (MessagingException e) {
            log.error("发送HTML邮件失败: {}", e.getMessage());
            throw new RuntimeException("发送邮件失败", e);
        }
    }

    /**
     * 发送验证码邮件(使用Thymeleaf模板)
     *
     * @param to 收件人
     * @param code 验证码
     * @param expirationMinutes 过期时间(分钟)
     */
    public void sendVerificationCodeEmail(String to, String code, int expirationMinutes) {
        try {
            Context context = new Context();
            context.setVariable("code", code);
            context.setVariable("expirationMinutes", expirationMinutes);

            String htmlContent = templateEngine.process("verification-code-email", context);

            sendHtmlEmail(to, "NUSHungry - 密码重置验证码", htmlContent);
        } catch (Exception e) {
            log.error("发送验证码邮件失败: {}", e.getMessage());
            throw new RuntimeException("发送验证码邮件失败", e);
        }
    }

    /**
     * 发送密码重置成功通知邮件
     *
     * @param to 收件人
     */
    public void sendPasswordResetSuccessEmail(String to) {
        try {
            Context context = new Context();
            context.setVariable("email", to);

            String htmlContent = templateEngine.process("password-reset-success-email", context);

            sendHtmlEmail(to, "NUSHungry - 密码重置成功", htmlContent);
        } catch (Exception e) {
            log.error("发送密码重置成功邮件失败: {}", e.getMessage());
            // 这里不抛出异常,因为密码已经重置成功,邮件发送失败不应影响主流程
        }
    }
}
