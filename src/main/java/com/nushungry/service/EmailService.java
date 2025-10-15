package com.nushungry.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:no-reply@nushungry.com}")
    private String defaultFromAddress;

    public void sendPasswordResetCode(String to, String code) {
        SimpleMailMessage mailMessage = new SimpleMailMessage();
        mailMessage.setTo(to);
        mailMessage.setFrom(defaultFromAddress);
        mailMessage.setSubject("NUSHungry Password Reset Verification Code");
        mailMessage.setText(buildPasswordResetBody(code));

        try {
            mailSender.send(mailMessage);
        } catch (Exception ex) {
            log.error("Failed to send password reset email to {}", to, ex);
            throw new IllegalStateException("Failed to send verification email. Please try again later.");
        }
    }

    private String buildPasswordResetBody(String code) {
        return "Hello,\n\n"
                + "We received a request to reset your password for NUSHungry.\n"
                + "Use the verification code below to complete the process:\n\n"
                + code + "\n\n"
                + "This code will expire shortly. If you did not request a password reset, please ignore this email.\n\n"
                + "Best regards,\n"
                + "NUSHungry Team";
    }
}
