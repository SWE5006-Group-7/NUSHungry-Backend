package com.nushungry.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.userservice.client.CafeteriaServiceClient;
import com.nushungry.userservice.client.ReviewServiceClient;
import com.nushungry.userservice.dto.*;
import com.nushungry.userservice.repository.RefreshTokenRepository;
import com.nushungry.userservice.repository.UserRepository;
import com.nushungry.userservice.service.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AuthController 集成测试
 * 使用 @SpringBootTest + H2 数据库
 * 策略: 使用真实 Service 和 H2 数据库,仅 Mock Feign Clients
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AuthController 集成测试")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    // Mock Feign Clients
    @MockBean
    private CafeteriaServiceClient cafeteriaServiceClient;

    @MockBean
    private ReviewServiceClient reviewServiceClient;

    @BeforeEach
    void setUp() {
        // 清理数据库
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ==================== 用户注册测试 ====================

    @Test
    @DisplayName("正常注册流程")
    void testRegister_Success() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("Test@123");
        request.setEmail("test@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        // 验证数据库
        assert userRepository.count() == 1;
        assert userRepository.findByUsername("testuser").isPresent();
    }

    @Test
    @DisplayName("邮箱重复校验")
    void testRegister_DuplicateEmail() throws Exception {
        // 先注册一个用户
        RegisterRequest firstRequest = new RegisterRequest();
        firstRequest.setUsername("testuser");
        firstRequest.setPassword("Test@123");
        firstRequest.setEmail("test@example.com");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(firstRequest)))
                .andExpect(status().isCreated());

        // 使用相同邮箱再次注册
        RegisterRequest duplicateRequest = new RegisterRequest();
        duplicateRequest.setUsername("testuser2");
        duplicateRequest.setPassword("Test@123");
        duplicateRequest.setEmail("test@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRequest)))
                .andExpect(status().isBadRequest());

        // 验证数据库 - 只有一个用户
        assert userRepository.count() == 1;
    }

    @Test
    @DisplayName("注册参数校验 - 用户名为空")
    void testRegister_BlankUsername() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setPassword("Test@123");
        request.setEmail("test@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        // 验证数据库 - 无用户创建
        assert userRepository.count() == 0;
    }

    @Test
    @DisplayName("注册参数校验 - 邮箱格式错误")
    void testRegister_InvalidEmail() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("Test@123");
        request.setEmail("invalid-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assert userRepository.count() == 0;
    }

    @Test
    @DisplayName("注册参数校验 - 密码强度不足")
    void testRegister_WeakPassword() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("testuser");
        request.setPassword("123");
        request.setEmail("test@example.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        assert userRepository.count() == 0;
    }

    // ==================== 用户登录测试 ====================

    @Test
    @DisplayName("正常登录流程")
    void testLogin_Success() throws Exception {
        // 先注册用户
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("Test@123");
        registerRequest.setEmail("test@example.com");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // 登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Test@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.refreshToken", notNullValue()))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    @Test
    @DisplayName("错误密码处理")
    void testLogin_WrongPassword() throws Exception {
        // 先注册用户
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("Test@123");
        registerRequest.setEmail("test@example.com");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // 使用错误密码登录
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("WrongPassword@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("用户不存在处理")
    void testLogin_UserNotFound() throws Exception {
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("nonexistent");
        loginRequest.setPassword("Test@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("登录参数校验 - 用户名为空")
    void testLogin_BlankUsername() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("");
        request.setPassword("Test@123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("登录参数校验 - 密码为空")
    void testLogin_BlankPassword() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("testuser");
        request.setPassword("");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ==================== 令牌刷新测试 ====================

    @Test
    @DisplayName("正常刷新令牌")
    void testRefreshToken_Success() throws Exception {
        // 先注册并登录获取令牌
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("Test@123");
        registerRequest.setEmail("test@example.com");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Test@123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // 提取 refreshToken
        String responseJson = loginResult.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseJson, AuthResponse.class);
        String refreshToken = authResponse.getRefreshToken();

        // 使用 refreshToken 刷新
        RefreshTokenRequest refreshRequest = new RefreshTokenRequest();
        refreshRequest.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(refreshRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", notNullValue()))
                .andExpect(jsonPath("$.expiresIn", notNullValue()));
    }

    @Test
    @DisplayName("刷新令牌无效")
    void testRefreshToken_Invalid() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-refresh-token");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ==================== 用户登出测试 ====================

    @Test
    @DisplayName("正常登出流程")
    void testLogout_Success() throws Exception {
        // 先注册并登录获取令牌
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setPassword("Test@123");
        registerRequest.setEmail("test@example.com");

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("Test@123");

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        // 提取 refreshToken
        String responseJson = loginResult.getResponse().getContentAsString();
        AuthResponse authResponse = objectMapper.readValue(responseJson, AuthResponse.class);
        String refreshToken = authResponse.getRefreshToken();

        // 登出
        RefreshTokenRequest logoutRequest = new RefreshTokenRequest();
        logoutRequest.setRefreshToken(refreshToken);

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isOk());

        // 验证 refreshToken 已失效 - 尝试使用应该失败
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(logoutRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("登出时令牌不存在")
    void testLogout_TokenNotFound() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("nonexistent-token");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("登出时令牌为空")
    void testLogout_EmptyToken() throws Exception {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
