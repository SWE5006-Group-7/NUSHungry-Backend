package com.nushungry.userservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.userservice.client.CafeteriaServiceClient;
import com.nushungry.userservice.client.ReviewServiceClient;
import com.nushungry.userservice.dto.*;
import com.nushungry.userservice.model.User;
import com.nushungry.userservice.model.UserRole;
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

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminUserController 集成测试
 * 使用 @SpringBootTest + H2 数据库
 * 策略: 使用真实 Service 和 H2 数据库,仅 Mock Feign Clients
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AdminUserController 集成测试")
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // Mock Feign Clients - 避免测试时初始化失败
    @MockBean
    private CafeteriaServiceClient cafeteriaServiceClient;

    @MockBean
    private ReviewServiceClient reviewServiceClient;

    private String adminToken;
    private User adminUser;

    @BeforeEach
    void setUp() {
        // 清空数据库
        userRepository.deleteAll();

        // 创建管理员用户并生成 token
        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@example.com");
        adminUser.setPassword(passwordEncoder.encode("Admin123!"));
        adminUser.setRole(UserRole.ROLE_ADMIN);
        adminUser = userRepository.save(adminUser);

        // 生成管理员 JWT token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", adminUser.getId());
        claims.put("role", adminUser.getRole().name());
        adminToken = jwtUtil.generateToken(adminUser.getUsername(), claims);
    }

    // ==================== GET /api/admin/users ====================

    @Test
    @DisplayName("获取用户列表 - 成功")
    void testGetUserList_Success() throws Exception {
        // 准备数据: 创建多个用户
        User user1 = new User();
        user1.setUsername("user1");
        user1.setEmail("user1@example.com");
        user1.setPassword(passwordEncoder.encode("Pass123!"));
        user1.setRole(UserRole.ROLE_USER);
        userRepository.save(user1);

        User user2 = new User();
        user2.setUsername("user2");
        user2.setEmail("user2@example.com");
        user2.setPassword(passwordEncoder.encode("Pass123!"));
        user2.setRole(UserRole.ROLE_USER);
        userRepository.save(user2);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(3))) // admin + 2 users
                .andExpect(jsonPath("$.totalItems").value(3))
                .andExpect(jsonPath("$.currentPage").value(0));
    }

    @Test
    @DisplayName("获取用户列表 - 未认证")
    void testGetUserList_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("获取用户列表 - 分页参数")
    void testGetUserList_WithPagination() throws Exception {
        // 创建多个用户
        for (int i = 1; i <= 15; i++) {
            User user = new User();
            user.setUsername("user" + i);
            user.setEmail("user" + i + "@example.com");
            user.setPassword(passwordEncoder.encode("Pass123!"));
            user.setRole(UserRole.ROLE_USER);
            userRepository.save(user);
        }

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("page", "0")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users", hasSize(5)))
                .andExpect(jsonPath("$.totalItems").value(16)) // 15 users + 1 admin
                .andExpect(jsonPath("$.totalPages").value(4));
    }

    // ==================== GET /api/admin/users/{id} ====================

    @Test
    @DisplayName("获取用户详情 - 成功")
    void testGetUserDetail_Success() throws Exception {
        // 准备数据
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setPassword(passwordEncoder.encode("Pass123!"));
        user.setRole(UserRole.ROLE_USER);
        user = userRepository.save(user);

        mockMvc.perform(get("/api/admin/users/" + user.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId()))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("testuser@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    @DisplayName("获取用户详情 - 用户不存在")
    void testGetUserDetail_NotFound() throws Exception {
        mockMvc.perform(get("/api/admin/users/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("获取用户详情 - 未认证")
    void testGetUserDetail_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/users/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== DELETE /api/admin/users/{id} ====================

    @Test
    @DisplayName("删除用户 - 成功")
    void testDeleteUser_Success() throws Exception {
        // 准备数据
        User user = new User();
        user.setUsername("todelete");
        user.setEmail("todelete@example.com");
        user.setPassword(passwordEncoder.encode("Pass123!"));
        user.setRole(UserRole.ROLE_USER);
        user = userRepository.save(user);

        Long userId = user.getId();

        mockMvc.perform(delete("/api/admin/users/" + userId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("用户删除成功"));

        // 验证数据库中用户已被删除
        assert !userRepository.existsById(userId);
    }

    @Test
    @DisplayName("删除用户 - 用户不存在")
    void testDeleteUser_NotFound() throws Exception {
        mockMvc.perform(delete("/api/admin/users/99999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("删除用户 - 未认证")
    void testDeleteUser_Unauthorized() throws Exception {
        mockMvc.perform(delete("/api/admin/users/1"))
                .andExpect(status().isForbidden());
    }

    // ==================== PATCH /api/admin/users/{id}/status ====================

    @Test
    @DisplayName("更新用户状态 - 禁用用户")
    void testUpdateUserStatus_Disable() throws Exception {
        // 准备数据
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setPassword(passwordEncoder.encode("Pass123!"));
        user.setRole(UserRole.ROLE_USER);
        user.setEnabled(true);
        user = userRepository.save(user);

        Map<String, Boolean> request = new HashMap<>();
        request.put("enabled", false);

        mockMvc.perform(patch("/api/admin/users/" + user.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("账户已禁用"))
                .andExpect(jsonPath("$.user.enabled").value(false));

        // 验证数据库状态
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assert !updatedUser.getEnabled();
    }

    @Test
    @DisplayName("更新用户状态 - 启用用户")
    void testUpdateUserStatus_Enable() throws Exception {
        // 准备数据
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setPassword(passwordEncoder.encode("Pass123!"));
        user.setRole(UserRole.ROLE_USER);
        user.setEnabled(false);
        user = userRepository.save(user);

        Map<String, Boolean> request = new HashMap<>();
        request.put("enabled", true);

        mockMvc.perform(patch("/api/admin/users/" + user.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("账户已启用"))
                .andExpect(jsonPath("$.user.enabled").value(true));

        // 验证数据库状态
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assert updatedUser.getEnabled();
    }

    @Test
    @DisplayName("更新用户状态 - 缺少参数")
    void testUpdateUserStatus_MissingParameter() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setPassword(passwordEncoder.encode("Pass123!"));
        user.setRole(UserRole.ROLE_USER);
        user = userRepository.save(user);

        Map<String, String> request = new HashMap<>();

        mockMvc.perform(patch("/api/admin/users/" + user.getId() + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("缺少enabled参数"));
    }

    @Test
    @DisplayName("更新用户状态 - 未认证")
    void testUpdateUserStatus_Unauthorized() throws Exception {
        Map<String, Boolean> request = new HashMap<>();
        request.put("enabled", false);

        mockMvc.perform(patch("/api/admin/users/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ==================== PUT /api/admin/users/{id}/role ====================

    @Test
    @DisplayName("修改用户角色 - 成功")
    void testUpdateUserRole_Success() throws Exception {
        // 准备数据
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setPassword(passwordEncoder.encode("Pass123!"));
        user.setRole(UserRole.ROLE_USER);
        user = userRepository.save(user);

        Map<String, String> request = new HashMap<>();
        request.put("role", "ROLE_ADMIN");

        mockMvc.perform(put("/api/admin/users/" + user.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("角色修改成功"))
                .andExpect(jsonPath("$.user.role").value("ROLE_ADMIN"));

        // 验证数据库状态
        User updatedUser = userRepository.findById(user.getId()).orElseThrow();
        assert updatedUser.getRole() == UserRole.ROLE_ADMIN;
    }

    @Test
    @DisplayName("修改用户角色 - 无效角色")
    void testUpdateUserRole_InvalidRole() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setPassword(passwordEncoder.encode("Pass123!"));
        user.setRole(UserRole.ROLE_USER);
        user = userRepository.save(user);

        Map<String, String> request = new HashMap<>();
        request.put("role", "INVALID_ROLE");

        mockMvc.perform(put("/api/admin/users/" + user.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("修改用户角色 - 缺少参数")
    void testUpdateUserRole_MissingParameter() throws Exception {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("testuser@example.com");
        user.setPassword(passwordEncoder.encode("Pass123!"));
        user.setRole(UserRole.ROLE_USER);
        user = userRepository.save(user);

        Map<String, String> request = new HashMap<>();

        mockMvc.perform(put("/api/admin/users/" + user.getId() + "/role")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("缺少role参数"));
    }

    @Test
    @DisplayName("修改用户角色 - 未认证")
    void testUpdateUserRole_Unauthorized() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("role", "ROLE_ADMIN");

        mockMvc.perform(put("/api/admin/users/1/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}
