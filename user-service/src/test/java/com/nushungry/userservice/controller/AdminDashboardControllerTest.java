package com.nushungry.userservice.controller;

import com.nushungry.userservice.AbstractControllerTest;
import com.nushungry.userservice.client.CafeteriaServiceClient;
import com.nushungry.userservice.client.ReviewServiceClient;
import com.nushungry.userservice.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminDashboardController 集成测试
 * 使用 @SpringBootTest + H2 数据库 + MockBean
 * 策略: 使用真实 Service 和 H2 数据库,Mock Feign Clients 返回值
 */
@DisplayName("AdminDashboardController 集成测试")
class AdminDashboardControllerTest extends AbstractControllerTest {

    private User adminUser;
    private String adminToken;

    @BeforeEach
    void setUp() {
        cleanDatabase();

        // 创建管理员用户
        adminUser = createAdminUser("admin", "admin@test.com");
        adminToken = generateToken(adminUser);

        // Mock Feign Client 默认返回值
        setupDefaultMockResponses();
    }

    /**
     * 设置默认的 Mock 响应,避免每个测试重复配置
     */
    private void setupDefaultMockResponses() {
        // Mock CafeteriaServiceClient
        when(cafeteriaServiceClient.getCafeteriaStats())
            .thenReturn(CafeteriaServiceClient.CafeteriaStatsResponse.builder()
                .totalCafeterias(5)
                .yesterdayCafeterias(4)
                .totalStalls(20)
                .yesterdayStalls(18)
                .build());

        // Mock ReviewServiceClient
        when(reviewServiceClient.getReviewStats())
            .thenReturn(ReviewServiceClient.ReviewStatsResponse.builder()
                .totalReviews(100)
                .yesterdayReviews(95)
                .todayReviews(10)
                .yesterdayReviewsForToday(8)
                .pendingComplaints(3)
                .totalComplaints(10)
                .build());

        when(reviewServiceClient.getLatestReviews())
            .thenReturn(Collections.emptyList());
    }

    // ==================== GET /api/admin/dashboard/stats ====================

    @Test
    @DisplayName("获取仪表盘统计数据 - 成功")
    void testGetDashboardStats_Success() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statsCards").exists())
            .andExpect(jsonPath("$.systemOverview").exists())
            .andExpect(jsonPath("$.userGrowthData").isArray())
            .andExpect(jsonPath("$.latestUsers").isArray())
            .andExpect(jsonPath("$.latestReviews").isArray());

        // 验证调用了外部服务
        verify(cafeteriaServiceClient, times(1)).getCafeteriaStats();
        verify(reviewServiceClient, times(2)).getReviewStats();  // getStatsCards() 和 getSystemOverview() 各调用1次
        verify(reviewServiceClient, times(1)).getLatestReviews();
    }

    @Test
    @DisplayName("获取仪表盘统计数据 - 未认证访问")
    void testGetDashboardStats_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats"))
            .andExpect(status().isForbidden());  // Spring Security 默认返回403,不是401
    }

    @Test
    @DisplayName("获取仪表盘统计数据 - 非管理员访问")
    void testGetDashboardStats_Forbidden() throws Exception {
        // 创建普通用户
        User user = createTestUser("user", "user@test.com");
        String userToken = generateToken(user);

        mockMvc.perform(get("/api/admin/dashboard/stats")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("获取仪表盘统计数据 - 外部服务失败时优雅降级")
    void testGetDashboardStats_ExternalServiceFailure() throws Exception {
        // Mock 外部服务抛出异常
        when(cafeteriaServiceClient.getCafeteriaStats())
            .thenThrow(new RuntimeException("Service unavailable"));
        when(reviewServiceClient.getReviewStats())
            .thenThrow(new RuntimeException("Service unavailable"));

        // 应该返回默认值,不应该抛出异常
        mockMvc.perform(get("/api/admin/dashboard/stats")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statsCards.totalCafeterias").value(0))
            .andExpect(jsonPath("$.statsCards.totalReviews").value(0));
    }

    // ==================== GET /api/admin/dashboard/stats/users ====================

    @Test
    @DisplayName("获取用户统计卡片 - 成功")
    void testGetUserStats_Success() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats/users")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalUsers").isNumber())
            .andExpect(jsonPath("$.userTrend").isNumber())
            .andExpect(jsonPath("$.totalCafeterias").value(5))
            .andExpect(jsonPath("$.totalStalls").value(20))
            .andExpect(jsonPath("$.totalReviews").value(100))
            .andExpect(jsonPath("$.todayOrders").value(10));

        verify(cafeteriaServiceClient, times(1)).getCafeteriaStats();
        verify(reviewServiceClient, times(1)).getReviewStats();
    }

    @Test
    @DisplayName("获取用户统计卡片 - 未认证访问")
    void testGetUserStats_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats/users"))
            .andExpect(status().isForbidden());  // Spring Security 默认返回403
    }

    @Test
    @DisplayName("获取用户统计卡片 - 非管理员访问")
    void testGetUserStats_Forbidden() throws Exception {
        User user = createTestUser("user", "user@test.com");
        String userToken = generateToken(user);

        mockMvc.perform(get("/api/admin/dashboard/stats/users")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    // ==================== GET /api/admin/dashboard/stats/system ====================

    @Test
    @DisplayName("获取系统概览 - 成功")
    void testGetSystemOverview_Success() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats/system")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.runningDays").isNumber())
            .andExpect(jsonPath("$.activeUsers").isNumber())
            .andExpect(jsonPath("$.activeUserPercentage").isNumber())
            .andExpect(jsonPath("$.pendingComplaints").value(3))
            .andExpect(jsonPath("$.pendingComplaintPercentage").isNumber())
            .andExpect(jsonPath("$.healthScore").isNumber())
            .andExpect(jsonPath("$.healthStatus").isString());

        verify(reviewServiceClient, times(1)).getReviewStats();
    }

    @Test
    @DisplayName("获取系统概览 - 未认证访问")
    void testGetSystemOverview_Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats/system"))
            .andExpect(status().isForbidden());  // Spring Security 默认返回403
    }

    @Test
    @DisplayName("获取系统概览 - 非管理员访问")
    void testGetSystemOverview_Forbidden() throws Exception {
        User user = createTestUser("user", "user@test.com");
        String userToken = generateToken(user);

        mockMvc.perform(get("/api/admin/dashboard/stats/system")
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("获取系统概览 - Review Service 失败时优雅降级")
    void testGetSystemOverview_ReviewServiceFailure() throws Exception {
        when(reviewServiceClient.getReviewStats())
            .thenThrow(new RuntimeException("Service unavailable"));

        mockMvc.perform(get("/api/admin/dashboard/stats/system")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.pendingComplaints").value(0))
            .andExpect(jsonPath("$.pendingComplaintPercentage").value(0));
    }

    // ==================== GET /api/admin/dashboard/user-growth ====================

    @Test
    @DisplayName("获取用户增长数据 - 成功")
    void testGetUserGrowth_Success() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(6);
        LocalDate endDate = LocalDate.now();

        mockMvc.perform(get("/api/admin/dashboard/user-growth")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(7))  // 7天数据
            .andExpect(jsonPath("$[0].date").isString())
            .andExpect(jsonPath("$[0].count").isNumber());
    }

    @Test
    @DisplayName("获取用户增长数据 - 包含测试数据")
    void testGetUserGrowth_WithTestData() throws Exception {
        // 创建一些测试用户
        createTestUser("user1", "user1@test.com");
        createTestUser("user2", "user2@test.com");

        LocalDate today = LocalDate.now();

        mockMvc.perform(get("/api/admin/dashboard/user-growth")
                .param("startDate", today.toString())
                .param("endDate", today.toString())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].date").value(today.toString()))
            .andExpect(jsonPath("$[0].count").value(greaterThanOrEqualTo(2)));  // 至少包含刚创建的2个用户
    }

    @Test
    @DisplayName("获取用户增长数据 - 未认证访问")
    void testGetUserGrowth_Unauthorized() throws Exception {
        LocalDate startDate = LocalDate.now().minusDays(6);
        LocalDate endDate = LocalDate.now();

        mockMvc.perform(get("/api/admin/dashboard/user-growth")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString()))
            .andExpect(status().isForbidden());  // Spring Security 默认返回403
    }

    @Test
    @DisplayName("获取用户增长数据 - 非管理员访问")
    void testGetUserGrowth_Forbidden() throws Exception {
        User user = createTestUser("user", "user@test.com");
        String userToken = generateToken(user);

        LocalDate startDate = LocalDate.now().minusDays(6);
        LocalDate endDate = LocalDate.now();

        mockMvc.perform(get("/api/admin/dashboard/user-growth")
                .param("startDate", startDate.toString())
                .param("endDate", endDate.toString())
                .header("Authorization", "Bearer " + userToken))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("获取用户增长数据 - 缺少参数")
    void testGetUserGrowth_MissingParameters() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/user-growth")
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("获取用户增长数据 - 日期格式错误")
    void testGetUserGrowth_InvalidDateFormat() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/user-growth")
                .param("startDate", "invalid-date")
                .param("endDate", LocalDate.now().toString())
                .header("Authorization", "Bearer " + adminToken))
            .andExpect(status().isBadRequest());
    }
}
