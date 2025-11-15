package com.nushungry.userservice.controller;

import com.nushungry.userservice.dto.ResetPasswordRequest;
import com.nushungry.userservice.dto.SendResetCodeRequest;
import com.nushungry.userservice.dto.VerifyResetCodeRequest;
import com.nushungry.userservice.model.User;
import com.nushungry.userservice.service.EmailService;
import com.nushungry.userservice.service.UserService;
import com.nushungry.userservice.service.VerificationCodeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 密码重置控制器
 */
@Tag(name = "密码重置", description = "密码找回和重置相关接口")
@RestController
@RequestMapping("/api/password")
@RequiredArgsConstructor
@Slf4j
public class PasswordResetController {

    private final VerificationCodeService verificationCodeService;
    private final UserService userService;
    private final EmailService emailService;

    /**
     * 发送密码重置验证码
     */
    @Operation(summary = "发送密码重置验证码", description = "向用户邮箱发送验证码")
    @PostMapping("/send-reset-code")
    public ResponseEntity<Map<String, Object>> sendResetCode(
            @Valid @RequestBody SendResetCodeRequest request,
            HttpServletRequest httpRequest) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 检查邮箱是否存在
            Optional<User> userOptional = userService.findByEmail(request.getEmail());
            if (userOptional.isEmpty()) {
                response.put("success", false);
                response.put("message", "该邮箱未注册");
                return ResponseEntity.badRequest().body(response);
            }

            // 获取客户端IP地址
            String ipAddress = getClientIp(httpRequest);

            // 发送验证码
            boolean sent = verificationCodeService.sendPasswordResetCode(
                    request.getEmail(), ipAddress);

            if (sent) {
                response.put("success", true);
                response.put("message", "验证码已发送至邮箱,请查收");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "发送验证码失败,请稍后重试");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 验证重置验证码
     */
    @Operation(summary = "验证重置验证码", description = "验证用户输入的验证码是否正确")
    @PostMapping("/verify-reset-code")
    public ResponseEntity<Map<String, Object>> verifyResetCode(
            @Valid @RequestBody VerifyResetCodeRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 只检查验证码是否有效，不标记为已使用
            boolean verified = verificationCodeService.checkPasswordResetCode(
                    request.getEmail(), request.getCode());

            if (verified) {
                response.put("success", true);
                response.put("message", "验证码验证成功");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "验证码错误或已过期");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "验证失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 重置密码
     */
    @Operation(summary = "重置密码", description = "使用验证码重置密码")
    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证验证码并标记为已使用
            boolean verified = verificationCodeService.verifyPasswordResetCode(
                    request.getEmail(), request.getCode());

            if (!verified) {
                response.put("success", false);
                response.put("message", "验证码错误或已过期");
                return ResponseEntity.badRequest().body(response);
            }

            // 重置密码
            userService.resetPassword(request.getEmail(), request.getNewPassword());

            // 发送密码重置成功通知邮件
            try {
                emailService.sendPasswordResetSuccessEmail(request.getEmail());
            } catch (Exception e) {
                log.error("发送密码重置成功邮件失败: {}", e.getMessage());
                // 不影响主流程
            }

            response.put("success", true);
            response.put("message", "密码重置成功,请使用新密码登录");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
