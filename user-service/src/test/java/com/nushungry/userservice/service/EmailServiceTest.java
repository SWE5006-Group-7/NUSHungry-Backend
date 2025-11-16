package com.nushungry.userservice.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * EmailService 单元测试
 * 使用轻量级 Mockito 测试,不启动 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private EmailService emailService;

    private MimeMessage mimeMessage;

    @BeforeEach
    void setUp() {
        // 创建 Mock MimeMessage
        mimeMessage = mock(MimeMessage.class);
    }

    // ==================== sendSimpleEmail() 测试 ====================

    @Test
    void testSendSimpleEmail_Success() {
        // Arrange
        String to = "test@example.com";
        String subject = "Test Subject";
        String text = "Test message content";

        // Act
        emailService.sendSimpleEmail(to, subject, text);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertNotNull(sentMessage);
        assertArrayEquals(new String[]{to}, sentMessage.getTo());
        assertEquals(subject, sentMessage.getSubject());
        assertEquals(text, sentMessage.getText());
    }

    @Test
    void testSendSimpleEmail_MailSenderThrowsException() {
        // Arrange
        doThrow(new RuntimeException("Mail server error"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            emailService.sendSimpleEmail("test@example.com", "Subject", "Text");
        });

        assertEquals("发送邮件失败", exception.getMessage());
        assertNotNull(exception.getCause());
        assertEquals("Mail server error", exception.getCause().getMessage());
    }

    // ==================== sendHtmlEmail() 测试 ====================

    @Test
    void testSendHtmlEmail_Success() throws MessagingException {
        // Arrange
        String to = "test@example.com";
        String subject = "HTML Email";
        String htmlContent = "<html><body><h1>Test HTML</h1></body></html>";

        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendHtmlEmail(to, subject, htmlContent);

        // Assert
        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendHtmlEmail_SendException() {
        // Arrange
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        // RuntimeException 不会被 MessagingException catch 块捕获,会直接抛出
        doThrow(new RuntimeException("Mail sending error"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            emailService.sendHtmlEmail("test@example.com", "Subject", "<html></html>");
        });

        // RuntimeException 直接抛出,不会被包装
        assertEquals("Mail sending error", exception.getMessage());
    }

    // ==================== sendVerificationCodeEmail() 测试 ====================

    @Test
    void testSendVerificationCodeEmail_Success() {
        // Arrange
        String to = "test@example.com";
        String code = "123456";
        int expirationMinutes = 10;
        String htmlContent = "<html><body>Verification code: 123456</body></html>";

        when(templateEngine.process(eq("verification-code-email"), any(Context.class)))
                .thenReturn(htmlContent);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendVerificationCodeEmail(to, code, expirationMinutes);

        // Assert
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("verification-code-email"), contextCaptor.capture());

        Context capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(code, capturedContext.getVariable("code"));
        assertEquals(expirationMinutes, capturedContext.getVariable("expirationMinutes"));

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendVerificationCodeEmail_TemplateProcessingException() {
        // Arrange
        when(templateEngine.process(eq("verification-code-email"), any(Context.class)))
                .thenThrow(new RuntimeException("Template error"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            emailService.sendVerificationCodeEmail("test@example.com", "123456", 10);
        });

        assertEquals("发送验证码邮件失败", exception.getMessage());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendVerificationCodeEmail_MailSendingException() {
        // Arrange
        String htmlContent = "<html><body>Code: 123456</body></html>";
        when(templateEngine.process(eq("verification-code-email"), any(Context.class)))
                .thenReturn(htmlContent);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Mail sending failed"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            emailService.sendVerificationCodeEmail("test@example.com", "123456", 10);
        });

        assertEquals("发送验证码邮件失败", exception.getMessage());
    }

    // ==================== sendPasswordResetSuccessEmail() 测试 ====================

    @Test
    void testSendPasswordResetSuccessEmail_Success() {
        // Arrange
        String to = "test@example.com";
        String htmlContent = "<html><body>Password reset successful</body></html>";

        when(templateEngine.process(eq("password-reset-success-email"), any(Context.class)))
                .thenReturn(htmlContent);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendPasswordResetSuccessEmail(to);

        // Assert
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("password-reset-success-email"), contextCaptor.capture());

        Context capturedContext = contextCaptor.getValue();
        assertNotNull(capturedContext);
        assertEquals(to, capturedContext.getVariable("email"));

        verify(mailSender).createMimeMessage();
        verify(mailSender).send(mimeMessage);
    }

    @Test
    void testSendPasswordResetSuccessEmail_ExceptionDoesNotPropagate() {
        // Arrange
        when(templateEngine.process(eq("password-reset-success-email"), any(Context.class)))
                .thenThrow(new RuntimeException("Template error"));

        // Act - 不应该抛出异常
        assertDoesNotThrow(() -> {
            emailService.sendPasswordResetSuccessEmail("test@example.com");
        });

        // Assert - 确认邮件没有发送
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendPasswordResetSuccessEmail_MailSendingExceptionDoesNotPropagate() {
        // Arrange
        String htmlContent = "<html><body>Success</body></html>";
        when(templateEngine.process(eq("password-reset-success-email"), any(Context.class)))
                .thenReturn(htmlContent);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new RuntimeException("Mail server down"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act - 不应该抛出异常(根据实现,密码重置成功不应因邮件失败而影响主流程)
        assertDoesNotThrow(() -> {
            emailService.sendPasswordResetSuccessEmail("test@example.com");
        });
    }

    // ==================== 边界条件测试 ====================

    @Test
    void testSendSimpleEmail_WithEmptyContent() {
        // Arrange
        String to = "test@example.com";
        String subject = "";
        String text = "";

        // Act
        emailService.sendSimpleEmail(to, subject, text);

        // Assert
        ArgumentCaptor<SimpleMailMessage> messageCaptor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        SimpleMailMessage sentMessage = messageCaptor.getValue();
        assertEquals("", sentMessage.getSubject());
        assertEquals("", sentMessage.getText());
    }

    @Test
    void testSendVerificationCodeEmail_WithZeroExpirationTime() {
        // Arrange
        String htmlContent = "<html></html>";
        when(templateEngine.process(eq("verification-code-email"), any(Context.class)))
                .thenReturn(htmlContent);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendVerificationCodeEmail("test@example.com", "000000", 0);

        // Assert
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("verification-code-email"), contextCaptor.capture());
        assertEquals(0, contextCaptor.getValue().getVariable("expirationMinutes"));
    }

    @Test
    void testSendVerificationCodeEmail_WithLongCode() {
        // Arrange
        String longCode = "123456789012345678901234567890";
        String htmlContent = "<html></html>";
        when(templateEngine.process(eq("verification-code-email"), any(Context.class)))
                .thenReturn(htmlContent);
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Act
        emailService.sendVerificationCodeEmail("test@example.com", longCode, 10);

        // Assert
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("verification-code-email"), contextCaptor.capture());
        assertEquals(longCode, contextCaptor.getValue().getVariable("code"));
    }
}
