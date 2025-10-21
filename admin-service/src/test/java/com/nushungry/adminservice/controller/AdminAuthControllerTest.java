package com.nushungry.adminservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.adminservice.filter.JwtAuthenticationFilter;
import com.nushungry.adminservice.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.is;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminAuthController 测试
 *
 * 使用 @WebMvcTest 进行轻量级控制器测试
 * 使用 @WithMockUser 模拟认证用户，避免复杂的JWT配置
 *
 * 注意: 这是简化的单元测试，主要测试控制器逻辑
 * 完整的认证流程测试在集成测试中进行 (AdminServiceIntegrationTest)
 */
@WebMvcTest(AdminAuthController.class)
@AutoConfigureMockMvc(addFilters = false)  // 禁用过滤器，简化测试
@ActiveProfiles("test")
class AdminAuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldReturnAuthenticatedMessageWhenUserIsAuthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/auth/test")
                        .with(csrf())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", is("认证成功")))
                .andExpect(jsonPath("$.username", is("admin")));
    }

    @Test
    void shouldReturnUnauthenticatedMessageWhenNoUserProvided() throws Exception {
        // 在禁用过滤器的情况下，没有认证信息时会返回"未认证"消息
        mockMvc.perform(get("/api/admin/auth/test")
                        .with(csrf())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", is("未认证")));
    }

    @Test
    @WithMockUser(username = "testuser", authorities = {"ROLE_USER"})
    void shouldAllowAccessWithAnyAuthenticatedUser() throws Exception {
        mockMvc.perform(get("/api/admin/auth/test")
                        .with(csrf())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", is("认证成功")))
                .andExpect(jsonPath("$.username", is("testuser")));
    }

    @Test
    @WithMockUser(username = "superadmin", authorities = {"ROLE_ADMIN", "ROLE_USER"})
    void shouldHandleMultipleRolesCorrectly() throws Exception {
        mockMvc.perform(get("/api/admin/auth/test")
                        .with(csrf())
                        .characterEncoding(StandardCharsets.UTF_8)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message", is("认证成功")))
                .andExpect(jsonPath("$.username", is("superadmin")));
    }
}
