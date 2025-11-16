package com.nushungry.userservice.service;

import com.nushungry.userservice.model.VerificationCode;
import com.nushungry.userservice.repository.VerificationCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VerificationCodeService 单元测试
 * 使用轻量级 Mockito 测试,不启动 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
class VerificationCodeServiceTest {

    @Mock
    private VerificationCodeRepository verificationCodeRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private VerificationCodeService verificationCodeService;

    private VerificationCode testVerificationCode;

    @BeforeEach
    void setUp() {
        // 使用反射设置私有字段(模拟 @Value 注入)
        ReflectionTestUtils.setField(verificationCodeService, "codeExpirationMs", 300000L); // 5分钟
        ReflectionTestUtils.setField(verificationCodeService, "codeLength", 6);

        // 创建测试验证码
        testVerificationCode = new VerificationCode();
        testVerificationCode.setId(1L);
        testVerificationCode.setEmail("test@example.com");
        testVerificationCode.setCode("123456");
        testVerificationCode.setType("PASSWORD_RESET");
        testVerificationCode.setCreatedAt(LocalDateTime.now());
        testVerificationCode.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        testVerificationCode.setUsed(false);
        testVerificationCode.setIpAddress("192.168.1.1");
    }

    // ==================== sendPasswordResetCode() 测试 ====================

    @Test
    void testSendPasswordResetCode_Success() {
        // Arrange
        String email = "test@example.com";
        String ipAddress = "192.168.1.1";

        when(verificationCodeRepository.countRecentCodes(eq(email), eq("PASSWORD_RESET"), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(verificationCodeRepository.save(any(VerificationCode.class))).thenReturn(testVerificationCode);
        doNothing().when(emailService).sendVerificationCodeEmail(anyString(), anyString(), anyInt());

        // Act
        boolean result = verificationCodeService.sendPasswordResetCode(email, ipAddress);

        // Assert
        assertTrue(result);

        // 验证保存了验证码
        ArgumentCaptor<VerificationCode> codeCaptor = ArgumentCaptor.forClass(VerificationCode.class);
        verify(verificationCodeRepository).save(codeCaptor.capture());

        VerificationCode savedCode = codeCaptor.getValue();
        assertEquals(email, savedCode.getEmail());
        assertEquals("PASSWORD_RESET", savedCode.getType());
        assertNotNull(savedCode.getCode());
        assertEquals(6, savedCode.getCode().length());
        assertFalse(savedCode.isUsed());
        assertEquals(ipAddress, savedCode.getIpAddress());

        // 验证发送了邮件
        verify(emailService).sendVerificationCodeEmail(eq(email), anyString(), eq(5));
    }

    @Test
    void testSendPasswordResetCode_RateLimitExceeded() {
        // Arrange
        String email = "test@example.com";
        when(verificationCodeRepository.countRecentCodes(eq(email), eq("PASSWORD_RESET"), any(LocalDateTime.class)))
                .thenReturn(5L); // 已达到上限

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            verificationCodeService.sendPasswordResetCode(email, "192.168.1.1");
        });

        assertTrue(exception.getMessage().contains("发送验证码过于频繁"));
        verify(verificationCodeRepository, never()).save(any(VerificationCode.class));
        verify(emailService, never()).sendVerificationCodeEmail(anyString(), anyString(), anyInt());
    }

    @Test
    void testSendPasswordResetCode_EmailServiceThrowsException() {
        // Arrange
        String email = "test@example.com";
        when(verificationCodeRepository.countRecentCodes(eq(email), eq("PASSWORD_RESET"), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(verificationCodeRepository.save(any(VerificationCode.class))).thenReturn(testVerificationCode);
        doThrow(new RuntimeException("Email service error"))
                .when(emailService).sendVerificationCodeEmail(anyString(), anyString(), anyInt());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            verificationCodeService.sendPasswordResetCode(email, "192.168.1.1");
        });

        assertTrue(exception.getMessage().contains("发送验证码失败"));
    }

    @Test
    void testSendPasswordResetCode_WithNullIpAddress() {
        // Arrange
        String email = "test@example.com";
        when(verificationCodeRepository.countRecentCodes(eq(email), eq("PASSWORD_RESET"), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(verificationCodeRepository.save(any(VerificationCode.class))).thenReturn(testVerificationCode);
        doNothing().when(emailService).sendVerificationCodeEmail(anyString(), anyString(), anyInt());

        // Act
        boolean result = verificationCodeService.sendPasswordResetCode(email, null);

        // Assert
        assertTrue(result);
        ArgumentCaptor<VerificationCode> codeCaptor = ArgumentCaptor.forClass(VerificationCode.class);
        verify(verificationCodeRepository).save(codeCaptor.capture());
        assertNull(codeCaptor.getValue().getIpAddress());
    }

    // ==================== checkPasswordResetCode() 测试 ====================

    @Test
    void testCheckPasswordResetCode_Success() {
        // Arrange
        String email = "test@example.com";
        String code = "123456";

        when(verificationCodeRepository.findLatestValidCode(eq(email), eq("PASSWORD_RESET"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testVerificationCode));

        // Act
        boolean result = verificationCodeService.checkPasswordResetCode(email, code);

        // Assert
        assertTrue(result);
        // checkPasswordResetCode 不会标记为已使用
        verify(verificationCodeRepository, never()).markAsUsed(anyLong(), any(LocalDateTime.class));
    }

    @Test
    void testCheckPasswordResetCode_CodeNotFound() {
        // Arrange
        String email = "test@example.com";
        when(verificationCodeRepository.findLatestValidCode(eq(email), eq("PASSWORD_RESET"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // Act
        boolean result = verificationCodeService.checkPasswordResetCode(email, "123456");

        // Assert
        assertFalse(result);
    }

    @Test
    void testCheckPasswordResetCode_CodeMismatch() {
        // Arrange
        String email = "test@example.com";
        String wrongCode = "999999";

        when(verificationCodeRepository.findLatestValidCode(eq(email), eq("PASSWORD_RESET"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testVerificationCode));

        // Act
        boolean result = verificationCodeService.checkPasswordResetCode(email, wrongCode);

        // Assert
        assertFalse(result);
    }

    @Test
    void testCheckPasswordResetCode_ExpiredCode() {
        // Arrange
        String email = "test@example.com";
        VerificationCode expiredCode = new VerificationCode();
        expiredCode.setEmail(email);
        expiredCode.setCode("123456");
        expiredCode.setType("PASSWORD_RESET");
        expiredCode.setCreatedAt(LocalDateTime.now().minusHours(1));
        expiredCode.setExpiresAt(LocalDateTime.now().minusMinutes(30)); // 已过期
        expiredCode.setUsed(false);

        when(verificationCodeRepository.findLatestValidCode(eq(email), eq("PASSWORD_RESET"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(expiredCode));

        // Act
        boolean result = verificationCodeService.checkPasswordResetCode(email, "123456");

        // Assert
        assertFalse(result);
    }

    @Test
    void testCheckPasswordResetCode_UsedCode() {
        // Arrange
        String email = "test@example.com";
        VerificationCode usedCode = new VerificationCode();
        usedCode.setEmail(email);
        usedCode.setCode("123456");
        usedCode.setType("PASSWORD_RESET");
        usedCode.setCreatedAt(LocalDateTime.now());
        usedCode.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        usedCode.setUsed(true); // 已使用
        usedCode.setUsedAt(LocalDateTime.now().minusMinutes(1));

        when(verificationCodeRepository.findLatestValidCode(eq(email), eq("PASSWORD_RESET"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(usedCode));

        // Act
        boolean result = verificationCodeService.checkPasswordResetCode(email, "123456");

        // Assert
        assertFalse(result);
    }

    // ==================== verifyPasswordResetCode() 测试 ====================

    @Test
    void testVerifyPasswordResetCode_Success() {
        // Arrange
        String email = "test@example.com";
        String code = "123456";

        when(verificationCodeRepository.findLatestValidCode(eq(email), eq("PASSWORD_RESET"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testVerificationCode));
        doNothing().when(verificationCodeRepository).markAsUsed(anyLong(), any(LocalDateTime.class));

        // Act
        boolean result = verificationCodeService.verifyPasswordResetCode(email, code);

        // Assert
        assertTrue(result);
        verify(verificationCodeRepository).markAsUsed(eq(1L), any(LocalDateTime.class));
    }

    @Test
    void testVerifyPasswordResetCode_CodeNotFound() {
        // Arrange
        String email = "test@example.com";
        when(verificationCodeRepository.findLatestValidCode(eq(email), eq("PASSWORD_RESET"), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        // Act
        boolean result = verificationCodeService.verifyPasswordResetCode(email, "123456");

        // Assert
        assertFalse(result);
        verify(verificationCodeRepository, never()).markAsUsed(anyLong(), any(LocalDateTime.class));
    }

    @Test
    void testVerifyPasswordResetCode_CodeMismatch() {
        // Arrange
        String email = "test@example.com";
        String wrongCode = "999999";

        when(verificationCodeRepository.findLatestValidCode(eq(email), eq("PASSWORD_RESET"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testVerificationCode));

        // Act
        boolean result = verificationCodeService.verifyPasswordResetCode(email, wrongCode);

        // Assert
        assertFalse(result);
        verify(verificationCodeRepository, never()).markAsUsed(anyLong(), any(LocalDateTime.class));
    }

    @Test
    void testVerifyPasswordResetCode_ExpiredCode() {
        // Arrange
        String email = "test@example.com";
        VerificationCode expiredCode = new VerificationCode();
        expiredCode.setId(1L);
        expiredCode.setEmail(email);
        expiredCode.setCode("123456");
        expiredCode.setType("PASSWORD_RESET");
        expiredCode.setCreatedAt(LocalDateTime.now().minusHours(1));
        expiredCode.setExpiresAt(LocalDateTime.now().minusMinutes(30)); // 已过期
        expiredCode.setUsed(false);

        when(verificationCodeRepository.findLatestValidCode(eq(email), eq("PASSWORD_RESET"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(expiredCode));

        // Act
        boolean result = verificationCodeService.verifyPasswordResetCode(email, "123456");

        // Assert
        assertFalse(result);
        verify(verificationCodeRepository, never()).markAsUsed(anyLong(), any(LocalDateTime.class));
    }

    // ==================== cleanupExpiredCodes() 测试 ====================

    @Test
    void testCleanupExpiredCodes_Success() {
        // Arrange
        doNothing().when(verificationCodeRepository).deleteExpiredCodes(any(LocalDateTime.class));

        // Act
        verificationCodeService.cleanupExpiredCodes();

        // Assert
        verify(verificationCodeRepository).deleteExpiredCodes(any(LocalDateTime.class));
    }

    @Test
    void testCleanupExpiredCodes_ExceptionDoesNotPropagate() {
        // Arrange
        doThrow(new RuntimeException("Database error"))
                .when(verificationCodeRepository).deleteExpiredCodes(any(LocalDateTime.class));

        // Act - 不应该抛出异常
        assertDoesNotThrow(() -> {
            verificationCodeService.cleanupExpiredCodes();
        });
    }

    // ==================== 边界条件测试 ====================

    @Test
    void testSendPasswordResetCode_RateLimitBoundary() {
        // Arrange
        String email = "test@example.com";
        when(verificationCodeRepository.countRecentCodes(eq(email), eq("PASSWORD_RESET"), any(LocalDateTime.class)))
                .thenReturn(4L); // 接近上限但未达到
        when(verificationCodeRepository.save(any(VerificationCode.class))).thenReturn(testVerificationCode);
        doNothing().when(emailService).sendVerificationCodeEmail(anyString(), anyString(), anyInt());

        // Act
        boolean result = verificationCodeService.sendPasswordResetCode(email, "192.168.1.1");

        // Assert
        assertTrue(result);
        verify(verificationCodeRepository).save(any(VerificationCode.class));
    }

    @Test
    void testGenerateCode_LengthCorrect() {
        // Arrange
        String email = "test@example.com";
        when(verificationCodeRepository.countRecentCodes(eq(email), eq("PASSWORD_RESET"), any(LocalDateTime.class)))
                .thenReturn(0L);
        when(verificationCodeRepository.save(any(VerificationCode.class))).thenReturn(testVerificationCode);
        doNothing().when(emailService).sendVerificationCodeEmail(anyString(), anyString(), anyInt());

        // Act
        verificationCodeService.sendPasswordResetCode(email, "192.168.1.1");

        // Assert
        ArgumentCaptor<VerificationCode> codeCaptor = ArgumentCaptor.forClass(VerificationCode.class);
        verify(verificationCodeRepository).save(codeCaptor.capture());
        String generatedCode = codeCaptor.getValue().getCode();

        assertEquals(6, generatedCode.length());
        assertTrue(generatedCode.matches("\\d{6}")); // 6位数字
    }

    @Test
    void testCheckPasswordResetCode_CaseSensitive() {
        // Arrange - 验证码应该是大小写敏感的(虽然当前实现是纯数字,但测试边界条件)
        String email = "test@example.com";
        testVerificationCode.setCode("123456");

        when(verificationCodeRepository.findLatestValidCode(eq(email), eq("PASSWORD_RESET"), any(LocalDateTime.class)))
                .thenReturn(Optional.of(testVerificationCode));

        // Act
        boolean result = verificationCodeService.checkPasswordResetCode(email, "123456");

        // Assert
        assertTrue(result);
    }
}
