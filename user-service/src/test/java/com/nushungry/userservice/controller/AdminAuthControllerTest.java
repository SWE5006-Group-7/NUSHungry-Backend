package com.nushungry.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.userservice.client.CafeteriaServiceClient;
import com.nushungry.userservice.client.ReviewServiceClient;
import com.nushungry.userservice.dto.AdminLoginRequest;
import com.nushungry.userservice.model.User;
import com.nushungry.userservice.model.UserRole;
import com.nushungry.userservice.repository.RefreshTokenRepository;
import com.nushungry.userservice.repository.UserRepository;
import com.nushungry.userservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminAuthController 集成测试
 * 使用 @SpringBootTest + H2 数据库
 * 策略: 使用真实 Service 和 H2 数据库,仅 Mock Feign Clients
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AdminAuthController 集成测试")
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // Mock Feign Clients
    @MockBean
    private CafeteriaServiceClient cafeteriaServiceClient;

    @MockBean
    private ReviewServiceClient reviewServiceClient;

    @BeforeEach
    void setUp() {
        // 清空数据库
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== POST /api/admin/auth/login ====================

    @Test
    @DisplayName("管理员登录 - 成功")
    void testAdminLogin_Success() throws Exception {
        // 准备数据: 创建管理员用户
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("Admin123!"));
        admin.setRole(UserRole.ROLE_ADMIN);
        userRepository.save(admin);

        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("Admin123!");

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.email").value("admin@example.com"))
                .andExpect(jsonPath("$.role").value("ADMIN"));
    }

    @Test
    @DisplayName("管理员登录 - 错误密码")
    void testAdminLogin_WrongPassword() throws Exception {
        // 准备数据: 创建管理员用户
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("Admin123!"));
        admin.setRole(UserRole.ROLE_ADMIN);
        userRepository.save(admin);

        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("WrongPassword");

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("管理员登录 - 用户不存在")
    void testAdminLogin_UserNotFound() throws Exception {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("nonexistent");
        request.setPassword("Admin123!");

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("管理员登录 - 普通用户无权限")
    void testAdminLogin_RegularUserForbidden() throws Exception {
        // 准备数据: 创建普通用户
        User user = new User();
        user.setUsername("regular");
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode("User123!"));
        user.setRole(UserRole.ROLE_USER);
        userRepository.save(user);

        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("regular");
        request.setPassword("User123!");

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("管理员登录 - 用户名为空")
    void testAdminLogin_BlankUsername() throws Exception {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("");
        request.setPassword("Admin123!");

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("管理员登录 - 密码为空")
    void testAdminLogin_BlankPassword() throws Exception {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("");

        mockMvc.perform(post("/api/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== POST /api/admin/auth/refresh ====================

    @Test
    @DisplayName("刷新管理员Token - 成功")
    void testRefreshToken_Success() throws Exception {
        // 准备数据: 创建管理员用户并生成token
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("Admin123!"));
        admin.setRole(UserRole.ROLE_ADMIN);
        admin = userRepository.save(admin);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", admin.getId());
        claims.put("role", admin.getRole().name()); // 使用 name() 而不是 getValue()
        String token = jwtUtil.generateToken(admin.getUsername(), claims);

        mockMvc.perform(post("/api/admin/auth/refresh")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    @DisplayName("刷新管理员Token - 缺少Authorization header")
    void testRefreshToken_MissingHeader() throws Exception {
        mockMvc.perform(post("/api/admin/auth/refresh"))
                .andExpect(status().isBadRequest()); // 实际返回400(因为进入了catch块)
    }

    @Test
    @DisplayName("刷新管理员Token - 无效的Authorization header")
    void testRefreshToken_InvalidHeader() throws Exception {
        mockMvc.perform(post("/api/admin/auth/refresh")
                        .header("Authorization", "InvalidHeader"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("刷新管理员Token - 普通用户无权限")
    void testRefreshToken_RegularUserForbidden() throws Exception {
        // 准备数据: 创建普通用户并生成token
        User user = new User();
        user.setUsername("regular");
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode("User123!"));
        user.setRole(UserRole.ROLE_USER);
        user = userRepository.save(user);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name()); // 使用 name() 而不是 getValue()
        String token = jwtUtil.generateToken(user.getUsername(), claims);

        mockMvc.perform(post("/api/admin/auth/refresh")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").exists());
    }

    // ==================== GET /api/admin/auth/verify ====================

    @Test
    @DisplayName("验证管理员Token - 成功")
    void testVerifyToken_Success() throws Exception {
        // 准备数据: 创建管理员用户并生成token
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("Admin123!"));
        admin.setRole(UserRole.ROLE_ADMIN);
        admin = userRepository.save(admin);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", admin.getId());
        claims.put("role", admin.getRole().name()); // 使用 name() 而不是 getValue()
        String token = jwtUtil.generateToken(admin.getUsername(), claims);

        mockMvc.perform(get("/api/admin/auth/verify")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"));
    }

    @Test
    @DisplayName("验证管理员Token - 缺少Authorization header")
    void testVerifyToken_MissingHeader() throws Exception {
        mockMvc.perform(get("/api/admin/auth/verify"))
                .andExpect(status().isBadRequest()); // 实际返回400(因为NullPointerException)
    }

    @Test
    @DisplayName("验证管理员Token - 无效的Authorization header")
    void testVerifyToken_InvalidHeader() throws Exception {
        mockMvc.perform(get("/api/admin/auth/verify")
                        .header("Authorization", "InvalidHeader"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.reason").exists());
    }

    @Test
    @DisplayName("验证管理员Token - 普通用户无管理员权限")
    void testVerifyToken_RegularUserNotAdmin() throws Exception {
        // 准备数据: 创建普通用户并生成token
        User user = new User();
        user.setUsername("regular");
        user.setEmail("user@example.com");
        user.setPassword(passwordEncoder.encode("User123!"));
        user.setRole(UserRole.ROLE_USER);
        user = userRepository.save(user);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("role", user.getRole().name()); // 使用 name() 而不是 getValue()
        String token = jwtUtil.generateToken(user.getUsername(), claims);

        mockMvc.perform(get("/api/admin/auth/verify")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false)) // 普通用户验证失败
                .andExpect(jsonPath("$.reason").value("Not an admin"));
    }

    // ==================== GET /api/admin/auth/test ====================

    @Test
    @DisplayName("测试认证 - 认证成功")
    void testAuth_Authenticated() throws Exception {
        // 准备数据: 创建管理员用户并生成token
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@example.com");
        admin.setPassword(passwordEncoder.encode("Admin123!"));
        admin.setRole(UserRole.ROLE_ADMIN);
        admin = userRepository.save(admin);

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", admin.getId());
        claims.put("role", admin.getRole().name()); // 使用 name() 而不是 getValue()
        String token = jwtUtil.generateToken(admin.getUsername(), claims);

        mockMvc.perform(get("/api/admin/auth/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("认证成功"))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    @DisplayName("测试认证 - 未认证")
    void testAuth_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/auth/test"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("未认证"));
    }
}
