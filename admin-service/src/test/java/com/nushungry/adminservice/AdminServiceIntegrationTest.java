package com.nushungry.adminservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.adminservice.dto.*;
import com.nushungry.adminservice.service.UserServiceClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * admin-service 集成测试
 * 
 * 测试策略：
 * 1. 使用 @MockBean 模拟外部依赖 (UserServiceClient)
 * 2. 使用 @WithMockUser 模拟认证用户
 * 3. 测试 API endpoints 的可用性和基本功能
 * 
 * 注意：这是一个端到端的集成测试，验证各组件能正确协作
 */
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        // 禁用 Feign 自动配置
        "spring.cloud.openfeign.enabled=false",
        // 禁用 RabbitMQ 自动配置  
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration"
    }
)
@AutoConfigureMockMvc(addFilters = false)  // 禁用Security filters 简化测试
@ActiveProfiles("test")
class AdminServiceIntegrationTest {

    @MockBean
    private UserServiceClient userServiceClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // 在每个测试前重置 mock
        reset(userServiceClient);
    }

    @Test
    void testHealthEndpoint() throws Exception {
        // 测试健康检查端点
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    void testDashboardStatsEndpoint() throws Exception {
        // 测试仪表盘统计端点（不需要外部依赖）
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statsCards").exists())
                .andExpect(jsonPath("$.systemOverview").exists());
    }

    @Test
    void testUserListEndpoint() throws Exception {
        // Mock user list response
        UserDTO mockUser = UserDTO.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .role("USER")
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
        
        UserListResponse mockResponse = UserListResponse.builder()
                .users(List.of(mockUser))
                .totalItems(1)
                .totalPages(1)
                .currentPage(0)
                .pageSize(10)
                .build();
        
        when(userServiceClient.getUserList(anyInt(), anyInt(), anyString(), anyString(), any()))
                .thenReturn(mockResponse);
        
        // 测试用户列表查询
        mockMvc.perform(get("/api/admin/users")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.users").isArray())
                .andExpect(jsonPath("$.users[0].username").value("testuser"));
    }

    @Test
    void testDashboardEndpointsAvailable() throws Exception {
        // 测试多个仪表盘端点的可用性
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isOk());
    }
}
