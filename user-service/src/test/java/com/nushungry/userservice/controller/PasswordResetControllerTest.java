package com.nushungry.userservice.controller;

import com.nushungry.userservice.AbstractControllerTest;
import com.nushungry.userservice.dto.ResetPasswordRequest;
import com.nushungry.userservice.dto.SendResetCodeRequest;
import com.nushungry.userservice.dto.VerifyResetCodeRequest;
import com.nushungry.userservice.model.User;
import com.nushungry.userservice.model.UserRole;
import com.nushungry.userservice.service.EmailService;
import com.nushungry.userservice.service.VerificationCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * PasswordResetController 集成测试
 * 使用 @SpringBootTest + H2 数据库
 * 策略: Mock VerificationCodeService 和 EmailService，使用真实的 UserService
 */
@DisplayName("PasswordResetController 集成测试")
class PasswordResetControllerTest extends AbstractControllerTest {

    @MockBean
    private VerificationCodeService verificationCodeService;

    @MockBean
    private EmailService emailService;

    private User testUser;

    @BeforeEach
    void setUp() {
        // 清理数据库
        cleanDatabase();

        // 创建测试用户
        testUser = createTestUser("testuser", "test@example.com", UserRole.ROLE_USER);
    }

    // ==================== 发送密码重置验证码测试 ====================

    @Test
    @DisplayName("发送密码重置验证码 - 成功")
    void testSendResetCode_Success() throws Exception {
        SendResetCodeRequest request = new SendResetCodeRequest();
        request.setEmail("test@example.com");

        // Mock VerificationCodeService 返回成功
        when(verificationCodeService.sendPasswordResetCode(anyString(), anyString()))
                .thenReturn(true);

        mockMvc.perform(post("/api/password/send-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("验证码已发送至邮箱,请查收"));
    }

    @Test
    @DisplayName("发送密码重置验证码 - 邮箱未注册")
    void testSendResetCode_EmailNotRegistered() throws Exception {
        SendResetCodeRequest request = new SendResetCodeRequest();
        request.setEmail("nonexistent@example.com");

        mockMvc.perform(post("/api/password/send-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("该邮箱未注册"));
    }

    @Test
    @DisplayName("发送密码重置验证码 - 发送失败")
    void testSendResetCode_SendFailed() throws Exception {
        SendResetCodeRequest request = new SendResetCodeRequest();
        request.setEmail("test@example.com");

        // Mock VerificationCodeService 返回失败
        when(verificationCodeService.sendPasswordResetCode(anyString(), anyString()))
                .thenReturn(false);

        mockMvc.perform(post("/api/password/send-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("发送验证码失败,请稍后重试"));
    }

    @Test
    @DisplayName("发送密码重置验证码 - 邮箱格式错误")
    void testSendResetCode_InvalidEmail() throws Exception {
        SendResetCodeRequest request = new SendResetCodeRequest();
        request.setEmail("invalid-email");

        mockMvc.perform(post("/api/password/send-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("发送密码重置验证码 - 邮箱为空")
    void testSendResetCode_EmptyEmail() throws Exception {
        SendResetCodeRequest request = new SendResetCodeRequest();
        request.setEmail("");

        mockMvc.perform(post("/api/password/send-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("发送密码重置验证码 - 频率限制")
    void testSendResetCode_RateLimit() throws Exception {
        SendResetCodeRequest request = new SendResetCodeRequest();
        request.setEmail("test@example.com");

        // Mock VerificationCodeService 抛出频率限制异常
        when(verificationCodeService.sendPasswordResetCode(anyString(), anyString()))
                .thenThrow(new RuntimeException("发送过于频繁,请稍后再试"));

        mockMvc.perform(post("/api/password/send-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("发送过于频繁,请稍后再试"));
    }

    // ==================== 验证重置验证码测试 ====================

    @Test
    @DisplayName("验证重置验证码 - 成功")
    void testVerifyResetCode_Success() throws Exception {
        VerifyResetCodeRequest request = new VerifyResetCodeRequest();
        request.setEmail("test@example.com");
        request.setCode("123456");

        // Mock VerificationCodeService 返回验证成功
        when(verificationCodeService.checkPasswordResetCode(anyString(), anyString()))
                .thenReturn(true);

        mockMvc.perform(post("/api/password/verify-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("验证码验证成功"));
    }

    @Test
    @DisplayName("验证重置验证码 - 验证码错误")
    void testVerifyResetCode_InvalidCode() throws Exception {
        VerifyResetCodeRequest request = new VerifyResetCodeRequest();
        request.setEmail("test@example.com");
        request.setCode("wrong-code");

        // Mock VerificationCodeService 返回验证失败
        when(verificationCodeService.checkPasswordResetCode(anyString(), anyString()))
                .thenReturn(false);

        mockMvc.perform(post("/api/password/verify-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("验证码错误或已过期"));
    }

    @Test
    @DisplayName("验证重置验证码 - 验证码为空")
    void testVerifyResetCode_EmptyCode() throws Exception {
        VerifyResetCodeRequest request = new VerifyResetCodeRequest();
        request.setEmail("test@example.com");
        request.setCode("");

        mockMvc.perform(post("/api/password/verify-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("验证重置验证码 - 验证码长度不正确")
    void testVerifyResetCode_InvalidCodeLength() throws Exception {
        VerifyResetCodeRequest request = new VerifyResetCodeRequest();
        request.setEmail("test@example.com");
        request.setCode("12");  // 长度不足

        mockMvc.perform(post("/api/password/verify-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== 重置密码测试 ====================

    @Test
    @DisplayName("重置密码 - 成功")
    void testResetPassword_Success() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setCode("123456");
        request.setNewPassword("NewPassword@123");

        // Mock VerificationCodeService 返回验证成功
        when(verificationCodeService.verifyPasswordResetCode(anyString(), anyString()))
                .thenReturn(true);

        mockMvc.perform(post("/api/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("密码重置成功,请使用新密码登录"));

        // 验证数据库中密码已更新（使用新密码应该可以匹配）
        User updatedUser = userRepository.findByEmail("test@example.com").orElseThrow();
        assert passwordEncoder.matches("NewPassword@123", updatedUser.getPassword());
    }

    @Test
    @DisplayName("重置密码 - 验证码错误")
    void testResetPassword_InvalidCode() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setCode("wrong-code");
        request.setNewPassword("NewPassword@123");

        // Mock VerificationCodeService 返回验证失败
        when(verificationCodeService.verifyPasswordResetCode(anyString(), anyString()))
                .thenReturn(false);

        mockMvc.perform(post("/api/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("验证码错误或已过期"));
    }

    @Test
    @DisplayName("重置密码 - 新密码为空")
    void testResetPassword_EmptyPassword() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setCode("123456");
        request.setNewPassword("");

        mockMvc.perform(post("/api/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("重置密码 - 新密码长度不足")
    void testResetPassword_PasswordTooShort() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setCode("123456");
        request.setNewPassword("123");  // 长度不足

        mockMvc.perform(post("/api/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("重置密码 - 新密码长度过长")
    void testResetPassword_PasswordTooLong() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("test@example.com");
        request.setCode("123456");
        request.setNewPassword("a".repeat(25));  // 长度超过20

        mockMvc.perform(post("/api/password/reset")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
