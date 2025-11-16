package com.nushungry.userservice.service;

import com.nushungry.userservice.dto.AuthResponse;
import com.nushungry.userservice.dto.LoginRequest;
import com.nushungry.userservice.dto.RegisterRequest;
import com.nushungry.userservice.dto.UserProfileResponse;
import com.nushungry.userservice.model.User;
import com.nushungry.userservice.model.UserRole;
import com.nushungry.userservice.repository.UserRepository;
import com.nushungry.userservice.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    public AuthResponse register(RegisterRequest request) {
        return register(request, null, null);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request, String ipAddress, String userAgent) {
        log.info("Registering new user: {}", request.getUsername());

        // Check if username already exists
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }

        // Check if email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        // Create new user
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setEnabled(true);
        user.setRole(UserRole.ROLE_USER);
        user.setLastLogin(LocalDateTime.now());

        User savedUser = userRepository.save(user);
        log.info("Successfully registered user: {}", savedUser.getUsername());

        // Generate Access Token with userId and role claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", savedUser.getId());
        claims.put("role", savedUser.getRole().getValue());
        String accessToken = jwtUtil.generateAccessToken(savedUser.getUsername(), claims);

        // Generate Refresh Token
        String refreshToken = refreshTokenService.createRefreshToken(savedUser.getId(), ipAddress, userAgent);

        AuthResponse response = new AuthResponse();
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtUtil.getAccessTokenExpiration());
        response.setId(savedUser.getId());
        response.setUsername(savedUser.getUsername());
        response.setEmail(savedUser.getEmail());
        response.setAvatarUrl(savedUser.getAvatarUrl());
        response.setRole(savedUser.getRole().getValue());

        return response;
    }

    public AuthResponse login(LoginRequest request) {
        return login(request, null, null);
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("User login attempt: {}", request.getUsername());

        // Authenticate user
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Load user details
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Update last login
        user.setLastLogin(LocalDateTime.now());
        userRepository.save(user);

        log.info("User successfully logged in: {}", user.getUsername());

        // Generate Access Token with userId and role claims
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().getValue());
        String accessToken = jwtUtil.generateAccessToken(user.getUsername(), claims);

        // Generate Refresh Token
        String refreshToken = refreshTokenService.createRefreshToken(user.getId(), ipAddress, userAgent);

        AuthResponse response = new AuthResponse();
        response.setToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setExpiresIn(jwtUtil.getAccessTokenExpiration());
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setRole(user.getRole().getValue());

        return response;
    }

    /**
     * 根据用户名查找用户
     * @param username 用户名
     * @return Optional<User>
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    /**
     * 根据邮箱查找用户
     * @param email 邮箱
     * @return Optional<User>
     */
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 根据ID查找用户
     * @param id 用户ID
     * @return Optional<User>
     */
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * 重置用户密码
     * @param email 邮箱
     * @param newPassword 新密码(明文)
     * @throws RuntimeException 如果用户不存在
     */
    @Transactional
    public void resetPassword(String email, String newPassword) {
        log.info("Resetting password for user: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        log.info("Password successfully reset for user: {}", email);
    }

    /**
     * 获取当前登录用户资料
     * @return UserProfileResponse
     */
    public UserProfileResponse getCurrentUserProfile() {
        User user = getCurrentUser();

        return new UserProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getCreatedAt()
        );
    }

    /**
     * 获取当前登录用户
     * @return User
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    /**
     * 更新用户头像
     * @param avatarUrl 头像URL
     */
    @Transactional
    public void updateAvatar(String avatarUrl) {
        User user = getCurrentUser();
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        log.info("Avatar updated for user: {}", user.getUsername());
    }
}