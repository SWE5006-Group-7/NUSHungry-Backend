package com.nushungry.adminservice.service;

import com.nushungry.adminservice.dto.DashboardStatsDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DashboardServiceTest {

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService();
    }

    @Test
    void shouldReturnPopulatedDashboardStats() {
        DashboardStatsDTO stats = dashboardService.getDashboardStats();

        assertThat(stats).isNotNull();
        assertThat(stats.getStatsCards()).isNotNull();
        assertThat(stats.getSystemOverview()).isNotNull();
        assertThat(stats.getUserGrowthData()).hasSize(7);
        assertThat(stats.getLatestUsers()).hasSize(5);
        assertThat(stats.getLatestReviews()).hasSize(5);
        assertThat(stats.getSystemOverview().getHealthScore()).isEqualTo(100);
        assertThat(stats.getSystemOverview().getHealthStatus()).isEqualTo("优秀");
    }

    @Test
    void shouldProvideDefaultSevenDayGrowthDataWhenRangeNotSpecified() {
        List<DashboardStatsDTO.UserGrowthData> growthData = dashboardService.getUserGrowthData(LocalDate.now().minusDays(6), LocalDate.now());

        assertThat(growthData).hasSize(7);
        assertThat(growthData.get(0).getDate()).isEqualTo("2025-10-09");
        assertThat(growthData.get(0).getCount()).isEqualTo(15);
        assertThat(growthData.get(6).getCount()).isEqualTo(20);
    }
}
