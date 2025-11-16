package com.nushungry.userservice.service;

import com.nushungry.userservice.dto.AdminLoginRequest;
import com.nushungry.userservice.dto.AdminLoginResponse;
import com.nushungry.userservice.dto.TokenVerifyResponse;
import com.nushungry.userservice.model.User;
import com.nushungry.userservice.model.UserRole;
import com.nushungry.userservice.repository.UserRepository;
import com.nushungry.userservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 管理员认证服务
 * 直接调用本地UserRepository进行认证，并生成管理员专用JWT Token
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 管理员登录
     * 验证管理员身份并返回JWT Token
     */
    @Transactional
    public AdminLoginResponse adminLogin(AdminLoginRequest request) {
        log.info("Admin login attempt for username: {}", request.getUsername());

        // 查找用户
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("用户名或密码错误"));

        // 验证密码
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Invalid password for user: {}", request.getUsername());
            throw new RuntimeException("用户名或密码错误");
        }

        // 验证是否为管理员
        if (user.getRole() != UserRole.ROLE_ADMIN) {
            log.warn("Non-admin user attempted admin login: {}", request.getUsername());
            throw new RuntimeException("无管理员权限");
        }

        // 验证账户是否启用
        if (!user.getEnabled()) {
            log.warn("Disabled admin user attempted login: {}", request.getUsername());
            throw new RuntimeException("账户已被禁用");
        }

        // 更新最后登录时间
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        // 生成管理员专用JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().getValue());
        claims.put("isAdmin", true);

        String token = jwtUtil.generateToken(user.getUsername(), claims);

        log.info("Admin successfully logged in: {}", request.getUsername());

        return AdminLoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpiration())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getValue())
                .isAdmin(true)
                .build();
    }

    /**
     * 刷新管理员Token
     */
    public AdminLoginResponse refreshToken(String token) {
        try {
            // 验证Token
            if (!jwtUtil.validateToken(token)) {
                throw new RuntimeException("Token无效");
            }

            if (jwtUtil.isTokenExpired(token)) {
                throw new RuntimeException("Token已过期");
            }

            // 提取用户信息
            String username = jwtUtil.getUsernameFromToken(token);
            Long userId = jwtUtil.getUserIdFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

            // 验证管理员权限
            if (!"ROLE_ADMIN".equals(role)) {
                throw new RuntimeException("无管理员权限");
            }

            // 生成新Token
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId);
            claims.put("username", username);
            claims.put("role", role);
            claims.put("isAdmin", true);

            String newToken = jwtUtil.generateToken(username, claims);

            log.info("Admin token refreshed for user: {}", username);

            return AdminLoginResponse.builder()
                    .token(newToken)
                    .tokenType("Bearer")
                    .expiresIn(jwtUtil.getExpiration())
                    .username(username)
                    .role(role)
                    .isAdmin(true)
                    .build();

        } catch (Exception e) {
            log.error("Error refreshing admin token: {}", e.getMessage());
            throw new RuntimeException("刷新Token失败：" + e.getMessage());
        }
    }

    /**
     * 验证管理员Token
     */
    public TokenVerifyResponse verifyToken(String token) {
        try {
            // 验证Token
            if (!jwtUtil.validateToken(token)) {
                return TokenVerifyResponse.builder()
                        .valid(false)
                        .reason("Invalid token")
                        .build();
            }

            if (jwtUtil.isTokenExpired(token)) {
                return TokenVerifyResponse.builder()
                        .valid(false)
                        .reason("Token expired")
                        .build();
            }

            // 提取用户信息
            String username = jwtUtil.getUsernameFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);

            // 验证管理员权限
            if (!"ROLE_ADMIN".equals(role)) {
                return TokenVerifyResponse.builder()
                        .valid(false)
                        .reason("Not an admin")
                        .build();
            }

            return TokenVerifyResponse.builder()
                    .valid(true)
                    .username(username)
                    .role(role)
                    .build();

        } catch (Exception e) {
            log.error("Error verifying admin token: {}", e.getMessage());
            return TokenVerifyResponse.builder()
                    .valid(false)
                    .reason("Verification failed")
                    .build();
        }
    }
}
