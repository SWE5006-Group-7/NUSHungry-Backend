package com.nushungry.adminservice.controller;

import com.nushungry.adminservice.dto.DashboardStatsDTO;
import com.nushungry.adminservice.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminDashboardController.class)
class AdminDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    private DashboardStatsDTO mockStats;

    @BeforeEach
    void setUp() {
        DashboardStatsDTO.StatsCards statsCards = DashboardStatsDTO.StatsCards.builder()
                .totalUsers(1250)
                .userTrend(12.5)
                .totalCafeterias(8)
                .cafeteriaTrend(0.0)
                .totalStalls(42)
                .stallTrend(5.2)
                .totalReviews(320)
                .reviewTrend(8.3)
                .todayOrders(25)
                .orderTrend(15.0)
                .build();

        DashboardStatsDTO.SystemOverview systemOverview = DashboardStatsDTO.SystemOverview.builder()
                .runningDays(30L)
                .activeUsers(850)
                .activeUserPercentage(68.0)
                .pendingComplaints(3)
                .pendingComplaintPercentage(12.0)
                .healthScore(90)
                .healthStatus("优秀")
                .build();

        mockStats = DashboardStatsDTO.builder()
                .statsCards(statsCards)
                .systemOverview(systemOverview)
                .build();
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldReturnDashboardStats() throws Exception {
        when(dashboardService.getDashboardStats()).thenReturn(mockStats);

        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statsCards.totalUsers").value(1250))
                .andExpect(jsonPath("$.statsCards.totalCafeterias").value(8))
                .andExpect(jsonPath("$.systemOverview.healthScore").value(90));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldReturnUserStats() throws Exception {
        when(dashboardService.getDashboardStats()).thenReturn(mockStats);

        mockMvc.perform(get("/api/admin/dashboard/stats/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalUsers").value(1250))
                .andExpect(jsonPath("$.userTrend").value(12.5));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldReturnSystemOverview() throws Exception {
        when(dashboardService.getDashboardStats()).thenReturn(mockStats);

        mockMvc.perform(get("/api/admin/dashboard/stats/system"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.runningDays").value(30))
                .andExpect(jsonPath("$.activeUsers").value(850))
                .andExpect(jsonPath("$.healthStatus").value("优秀"));
    }

    @Test
    @WithMockUser(username = "admin", authorities = {"ROLE_ADMIN"})
    void shouldReturnUserGrowthData() throws Exception {
        List<DashboardStatsDTO.UserGrowthData> growthData = List.of(
                DashboardStatsDTO.UserGrowthData.builder().date("2025-10-01").count(15).build(),
                DashboardStatsDTO.UserGrowthData.builder().date("2025-10-02").count(22).build()
        );

        when(dashboardService.getUserGrowthData(any(LocalDate.class), any(LocalDate.class)))
                .thenReturn(growthData);

        mockMvc.perform(get("/api/admin/dashboard/user-growth")
                        .param("startDate", "2025-10-01")
                        .param("endDate", "2025-10-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].date").value("2025-10-01"))
                .andExpect(jsonPath("$[0].count").value(15))
                .andExpect(jsonPath("$[1].date").value("2025-10-02"))
                .andExpect(jsonPath("$[1].count").value(22));
    }

    @Test
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user", authorities = {"ROLE_USER"})
    void shouldReturn403WhenNotAdmin() throws Exception {
        mockMvc.perform(get("/api/admin/dashboard/stats"))
                .andExpect(status().isForbidden());
    }
}
