package com.nushungry.userservice.controller;

import com.nushungry.userservice.dto.AdminLoginRequest;
import com.nushungry.userservice.dto.AdminLoginResponse;
import com.nushungry.userservice.dto.TokenVerifyResponse;
import com.nushungry.userservice.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 管理员认证控制器
 * 提供管理员专用的登录、Token刷新和验证接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Admin Auth", description = "管理员认证相关接口")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    /**
     * 管理员登录
     * 只允许拥有ROLE_ADMIN角色的用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "管理员专用登录接口，验证管理员身份并返回JWT token")
    public ResponseEntity<?> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        try {
            log.info("Admin login attempt for username: {}", request.getUsername());
            AdminLoginResponse response = adminAuthService.adminLogin(request);
            log.info("Admin login successful for username: {}", request.getUsername());
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.warn("Admin login failed for username {}: {}", request.getUsername(), e.getMessage());

            if (e.getMessage().contains("用户名或密码错误")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", e.getMessage()));
            } else if (e.getMessage().contains("无管理员权限")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", e.getMessage()));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("error", "登录失败，请稍后重试"));
            }
        }
    }

    /**
     * 刷新管理员Token
     * 使用现有token刷新获取新的token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新管理员Token", description = "使用现有token刷新获取新的token")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "无效的Authorization header"));
            }

            String token = authHeader.substring(7);
            AdminLoginResponse response = adminAuthService.refreshToken(token);

            log.info("Admin token refreshed successfully");
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Token refresh error: {}", e.getMessage());

            if (e.getMessage().contains("Token已过期")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token已过期，请重新登录"));
            } else if (e.getMessage().contains("无管理员权限")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "无权限刷新管理员token"));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "刷新token失败"));
            }
        }
    }

    /**
     * 验证管理员Token
     * 验证当前token是否有效且具有管理员权限
     */
    @GetMapping("/verify")
    @Operation(summary = "验证管理员Token", description = "验证当前token是否有效且具有管理员权限")
    public ResponseEntity<TokenVerifyResponse> verifyToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.ok(TokenVerifyResponse.builder()
                        .valid(false)
                        .reason("Invalid Authorization header")
                        .build());
            }

            String token = authHeader.substring(7);
            TokenVerifyResponse response = adminAuthService.verifyToken(token);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Token verification error: {}", e.getMessage());
            return ResponseEntity.ok(TokenVerifyResponse.builder()
                    .valid(false)
                    .reason("Verification failed")
                    .build());
        }
    }

    /**
     * 测试认证（保留用于测试）
     */
    @GetMapping("/test")
    @Operation(summary = "测试认证", description = "测试JWT认证是否正常工作")
    public ResponseEntity<Map<String, Object>> testAuth() {
        // 从 SecurityContext 获取认证信息
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.ok(Map.of("message", "未认证"));
        }

        String username = authentication.getName();
        return ResponseEntity.ok(Map.of(
            "message", "认证成功",
            "username", username
        ));
    }
}
