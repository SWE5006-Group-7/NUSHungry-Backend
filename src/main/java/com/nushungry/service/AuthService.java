package com.nushungry.service;

import com.nushungry.dto.LoginRequestDTO;
import com.nushungry.dto.LoginResponseDTO;
import com.nushungry.model.User;
import com.nushungry.model.UserRole;
import com.nushungry.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证服务
 * 处理用户登录、token生成等认证相关业务逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 普通用户登录
     * @param loginRequest 登录请求
     * @return 登录响应（包含token和用户信息）
     * @throws BadCredentialsException 用户名或密码错误
     * @throws UsernameNotFoundException 用户不存在
     */
    public LoginResponseDTO login(LoginRequestDTO loginRequest) {
        // 1. 验证用户凭证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // 2. 获取用户信息
        User user = userService.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        // 3. 检查账户是否启用
        if (!user.isEnabled()) {
            throw new RuntimeException("账户已被禁用");
        }

        // 4. 生成JWT token
        String token = generateToken(user);

        // 5. 构建响应
        return buildLoginResponse(token, user);
    }

    /**
     * 管理员登录
     * 只允许拥有ROLE_ADMIN角色的用户登录
     * @param loginRequest 登录请求
     * @return 登录响应
     * @throws BadCredentialsException 用户名或密码错误
     * @throws RuntimeException 无管理员权限或账户被禁用
     */
    public LoginResponseDTO adminLogin(LoginRequestDTO loginRequest) {
        // 1. 验证用户凭证
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                )
        );

        // 2. 获取用户信息
        User user = userService.findByUsername(loginRequest.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        // 3. 检查是否有管理员权限
        if (user.getRole() != UserRole.ROLE_ADMIN) {
            log.warn("Non-admin user attempted admin login: {}", user.getUsername());
            throw new RuntimeException("您没有管理员权限");
        }

        // 4. 检查账户是否启用
        if (!user.isEnabled()) {
            log.warn("Disabled admin account attempted login: {}", user.getUsername());
            throw new RuntimeException("账户已被禁用");
        }

        // 5. 生成包含管理员标识的JWT token
        String token = generateAdminToken(user);

        // 6. 记录日志
        log.info("Admin login successful for user: {}", user.getUsername());

        // 7. 构建响应
        return buildLoginResponse(token, user);
    }

    /**
     * 刷新Token
     * @param token 当前token
     * @return 新的token
     */
    public String refreshToken(String token) {
        String username = jwtUtil.extractUsername(token);

        if (username != null && jwtUtil.validateToken(token, username)) {
            User user = userService.findByUsername(username)
                    .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

            // 生成新token
            return generateToken(user);
        }

        throw new RuntimeException("无效的token");
    }

    /**
     * 验证Token
     * @param token JWT token
     * @return 是否有效
     */
    public boolean verifyToken(String token) {
        try {
            String username = jwtUtil.extractUsername(token);
            if (username != null) {
                User user = userService.findByUsername(username).orElse(null);
                return user != null && jwtUtil.validateToken(token, username);
            }
            return false;
        } catch (Exception e) {
            log.error("Token verification failed", e);
            return false;
        }
    }

    /**
     * 生成普通用户Token
     */
    private String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().getValue());

        return jwtUtil.generateTokenWithClaims(user.getUsername(), claims);
    }

    /**
     * 生成管理员Token（包含isAdmin标识）
     */
    private String generateAdminToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("role", user.getRole().getValue());
        claims.put("isAdmin", true);

        return jwtUtil.generateTokenWithClaims(user.getUsername(), claims);
    }

    /**
     * 构建登录响应
     */
    private LoginResponseDTO buildLoginResponse(String token, User user) {
        LoginResponseDTO.UserInfoDTO userInfo = LoginResponseDTO.UserInfoDTO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getValue())
                .avatarUrl(user.getAvatarUrl())
                .build();

        return LoginResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getJwtExpiration())
                .user(userInfo)
                .build();
    }
}
