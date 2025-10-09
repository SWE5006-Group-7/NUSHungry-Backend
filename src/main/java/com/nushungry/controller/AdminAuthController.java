package com.nushungry.controller;

import com.nushungry.dto.LoginRequestDTO;
import com.nushungry.dto.LoginResponseDTO;
import com.nushungry.model.User;
import com.nushungry.model.UserRole;
import com.nushungry.service.AuthService;
import com.nushungry.service.UserService;
import com.nushungry.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 管理员认证控制器
 * 提供管理员专用的登录接口
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Admin Auth", description = "管理员认证相关接口")
public class AdminAuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 管理员登录
     * 只允许拥有ROLE_ADMIN角色的用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "管理员登录", description = "管理员专用登录接口，验证管理员身份并返回JWT token")
    public ResponseEntity<?> adminLogin(@Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            LoginResponseDTO response = authService.adminLogin(loginRequest);
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException e) {
            log.warn("Failed admin login attempt for username: {}", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "用户名或密码错误"));
        } catch (UsernameNotFoundException e) {
            log.warn("Admin login attempt with non-existent username: {}", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "用户名或密码错误"));
        } catch (RuntimeException e) {
            log.warn("Admin login failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Admin login error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "登录失败，请稍后重试"));
        }
    }

    /**
     * 管理员token刷新
     * 刷新管理员的JWT token
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新管理员Token", description = "使用现有token刷新获取新的token")
    public ResponseEntity<?> refreshToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String username = jwtUtil.extractUsername(token);

                if (username != null && jwtUtil.validateToken(token, username)) {
                    User user = userService.findByUsername(username)
                            .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

                    // 验证是否为管理员
                    if (user.getRole() != UserRole.ROLE_ADMIN) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of("error", "无权限刷新管理员token"));
                    }

                    // 生成新token
                    Map<String, Object> claims = new HashMap<>();
                    claims.put("userId", user.getId());
                    claims.put("username", user.getUsername());
                    claims.put("email", user.getEmail());
                    claims.put("role", user.getRole().getValue());
                    claims.put("isAdmin", true);

                    String newToken = jwtUtil.generateTokenWithClaims(username, claims);

                    Map<String, Object> response = new HashMap<>();
                    response.put("token", newToken);
                    response.put("tokenType", "Bearer");
                    response.put("expiresIn", jwtUtil.getJwtExpiration());

                    return ResponseEntity.ok(response);
                }
            }

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "无效的token"));
        } catch (Exception e) {
            log.error("Token refresh error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "刷新token失败"));
        }
    }

    /**
     * 验证管理员token
     * 用于前端验证当前token是否有效
     */
    @GetMapping("/verify")
    @Operation(summary = "验证管理员Token", description = "验证当前token是否有效且具有管理员权限")
    public ResponseEntity<?> verifyToken(@RequestHeader("Authorization") String authHeader) {
        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                String username = jwtUtil.extractUsername(token);

                if (username != null && jwtUtil.validateToken(token, username)) {
                    User user = userService.findByUsername(username)
                            .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

                    // 验证是否为管理员
                    if (user.getRole() != UserRole.ROLE_ADMIN) {
                        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                .body(Map.of(
                                        "valid", false,
                                        "reason", "Not an admin"
                                ));
                    }

                    return ResponseEntity.ok(Map.of(
                            "valid", true,
                            "username", username,
                            "role", user.getRole().getValue()
                    ));
                }
            }

            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "reason", "Invalid token"
            ));
        } catch (Exception e) {
            log.error("Token verification error", e);
            return ResponseEntity.ok(Map.of(
                    "valid", false,
                    "reason", "Verification failed"
            ));
        }
    }
}