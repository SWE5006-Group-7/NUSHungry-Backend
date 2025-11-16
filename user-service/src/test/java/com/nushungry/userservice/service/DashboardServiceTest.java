package com.nushungry.userservice.service;

import com.nushungry.userservice.client.CafeteriaServiceClient;
import com.nushungry.userservice.client.ReviewServiceClient;
import com.nushungry.userservice.dto.DashboardStatsDTO;
import com.nushungry.userservice.model.User;
import com.nushungry.userservice.model.UserRole;
import com.nushungry.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * DashboardService 单元测试
 * 使用轻量级 Mockito 测试,不启动 Spring 容器
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CafeteriaServiceClient cafeteriaServiceClient;

    @Mock
    private ReviewServiceClient reviewServiceClient;

    @InjectMocks
    private DashboardService dashboardService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(UserRole.ROLE_USER);
        testUser.setAvatarUrl("http://example.com/avatar.jpg");
        testUser.setEnabled(true);
        testUser.setCreatedAt(LocalDateTime.now());
        testUser.setLastLogin(LocalDateTime.now());
    }

    // ==================== getDashboardStats() 测试 ====================

    @Test
    void testGetDashboardStats_Success() {
        // Arrange
        setupMocksForSuccess();

        // Act
        DashboardStatsDTO result = dashboardService.getDashboardStats();

        // Assert
        assertNotNull(result);
        assertNotNull(result.getStatsCards());
        assertNotNull(result.getSystemOverview());
        assertNotNull(result.getUserGrowthData());
        assertNotNull(result.getLatestUsers());
        assertNotNull(result.getLatestReviews());
    }

    // ==================== getStatsCards() 测试 ====================

    @Test
    void testGetStatsCards_AllServicesHealthy() {
        // Arrange
        // 用户数据
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(95L);

        // Cafeteria Service 数据
        CafeteriaServiceClient.CafeteriaStatsResponse cafeteriaStats =
                CafeteriaServiceClient.CafeteriaStatsResponse.builder()
                        .totalCafeterias(10)
                        .yesterdayCafeterias(9)
                        .totalStalls(50)
                        .yesterdayStalls(48)
                        .build();
        when(cafeteriaServiceClient.getCafeteriaStats()).thenReturn(cafeteriaStats);

        // Review Service 数据
        ReviewServiceClient.ReviewStatsResponse reviewStats =
                ReviewServiceClient.ReviewStatsResponse.builder()
                        .totalReviews(500)
                        .yesterdayReviews(480)
                        .todayReviews(30)
                        .yesterdayReviewsForToday(25)
                        .build();
        when(reviewServiceClient.getReviewStats()).thenReturn(reviewStats);

        // Act
        DashboardStatsDTO.StatsCards result = dashboardService.getStatsCards();

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getTotalUsers());
        assertEquals(10, result.getTotalCafeterias());
        assertEquals(50, result.getTotalStalls());
        assertEquals(500, result.getTotalReviews());
        assertEquals(30, result.getTodayOrders());

        // 验证趋势计算
        assertTrue(result.getUserTrend() > 0); // 100 vs 95
        assertTrue(result.getCafeteriaTrend() > 0); // 10 vs 9
    }

    @Test
    void testGetStatsCards_CafeteriaServiceFailure() {
        // Arrange
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(95L);

        // Cafeteria Service 失败
        when(cafeteriaServiceClient.getCafeteriaStats()).thenThrow(new RuntimeException("Service unavailable"));

        // Review Service 正常
        ReviewServiceClient.ReviewStatsResponse reviewStats =
                ReviewServiceClient.ReviewStatsResponse.builder()
                        .totalReviews(500)
                        .yesterdayReviews(480)
                        .todayReviews(30)
                        .yesterdayReviewsForToday(25)
                        .build();
        when(reviewServiceClient.getReviewStats()).thenReturn(reviewStats);

        // Act
        DashboardStatsDTO.StatsCards result = dashboardService.getStatsCards();

        // Assert
        assertNotNull(result);
        assertEquals(100, result.getTotalUsers());
        // Cafeteria 数据应该是默认值0
        assertEquals(0, result.getTotalCafeterias());
        assertEquals(0, result.getTotalStalls());
        // Review 数据应该正常
        assertEquals(500, result.getTotalReviews());
    }

    @Test
    void testGetStatsCards_ReviewServiceFailure() {
        // Arrange
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(95L);

        // Cafeteria Service 正常
        CafeteriaServiceClient.CafeteriaStatsResponse cafeteriaStats =
                CafeteriaServiceClient.CafeteriaStatsResponse.builder()
                        .totalCafeterias(10)
                        .yesterdayCafeterias(9)
                        .totalStalls(50)
                        .yesterdayStalls(48)
                        .build();
        when(cafeteriaServiceClient.getCafeteriaStats()).thenReturn(cafeteriaStats);

        // Review Service 失败
        when(reviewServiceClient.getReviewStats()).thenThrow(new RuntimeException("Service down"));

        // Act
        DashboardStatsDTO.StatsCards result = dashboardService.getStatsCards();

        // Assert
        assertNotNull(result);
        assertEquals(10, result.getTotalCafeterias());
        // Review 数据应该是默认值0
        assertEquals(0, result.getTotalReviews());
        assertEquals(0, result.getTodayOrders());
    }

    @Test
    void testGetStatsCards_AllServicesFail() {
        // Arrange
        when(userRepository.count()).thenReturn(50L);
        when(userRepository.countByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(50L);
        when(cafeteriaServiceClient.getCafeteriaStats()).thenThrow(new RuntimeException("Error"));
        when(reviewServiceClient.getReviewStats()).thenThrow(new RuntimeException("Error"));

        // Act
        DashboardStatsDTO.StatsCards result = dashboardService.getStatsCards();

        // Assert
        assertNotNull(result);
        assertEquals(50, result.getTotalUsers());
        assertEquals(0, result.getTotalCafeterias());
        assertEquals(0, result.getTotalStalls());
        assertEquals(0, result.getTotalReviews());
    }

    // ==================== getSystemOverview() 测试 ====================

    @Test
    void testGetSystemOverview_Success() {
        // Arrange
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByLastLoginAfter(any(LocalDateTime.class))).thenReturn(60L);

        ReviewServiceClient.ReviewStatsResponse reviewStats =
                ReviewServiceClient.ReviewStatsResponse.builder()
                        .pendingComplaints(10)
                        .totalComplaints(100)
                        .build();
        when(reviewServiceClient.getReviewStats()).thenReturn(reviewStats);

        // Act
        DashboardStatsDTO.SystemOverview result = dashboardService.getSystemOverview();

        // Assert
        assertNotNull(result);
        assertTrue(result.getRunningDays() > 0);
        assertEquals(60, result.getActiveUsers());
        assertEquals(60.0, result.getActiveUserPercentage(), 0.1);
        assertEquals(10, result.getPendingComplaints());
        assertEquals(10.0, result.getPendingComplaintPercentage(), 0.1);
        assertNotNull(result.getHealthStatus());
        assertTrue(result.getHealthScore() >= 0 && result.getHealthScore() <= 100);
    }

    @Test
    void testGetSystemOverview_ReviewServiceFailure() {
        // Arrange
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByLastLoginAfter(any(LocalDateTime.class))).thenReturn(60L);
        when(reviewServiceClient.getReviewStats()).thenThrow(new RuntimeException("Service error"));

        // Act
        DashboardStatsDTO.SystemOverview result = dashboardService.getSystemOverview();

        // Assert
        assertNotNull(result);
        assertEquals(60, result.getActiveUsers());
        // 投诉数据应该是默认值0
        assertEquals(0, result.getPendingComplaints());
        assertEquals(0.0, result.getPendingComplaintPercentage(), 0.1);
    }

    @Test
    void testGetSystemOverview_NoActiveUsers() {
        // Arrange
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByLastLoginAfter(any(LocalDateTime.class))).thenReturn(0L);

        ReviewServiceClient.ReviewStatsResponse reviewStats =
                ReviewServiceClient.ReviewStatsResponse.builder()
                        .pendingComplaints(0)
                        .totalComplaints(0)
                        .build();
        when(reviewServiceClient.getReviewStats()).thenReturn(reviewStats);

        // Act
        DashboardStatsDTO.SystemOverview result = dashboardService.getSystemOverview();

        // Assert
        assertEquals(0, result.getActiveUsers());
        assertEquals(0.0, result.getActiveUserPercentage(), 0.1);
        // 健康分数应该较低(活跃用户<30%)
        assertTrue(result.getHealthScore() <= 80);
    }

    // ==================== getUserGrowthData() 测试 ====================

    @Test
    void testGetUserGrowthData_WithDateRange() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusDays(6);
        LocalDate endDate = LocalDate.now();

        when(userRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(10L);

        // Act
        List<DashboardStatsDTO.UserGrowthData> result = dashboardService.getUserGrowthData(startDate, endDate);

        // Assert
        assertNotNull(result);
        assertEquals(7, result.size()); // 7天数据
        result.forEach(data -> {
            assertNotNull(data.getDate());
            assertEquals(10, data.getCount());
        });
    }

    @Test
    void testGetUserGrowthData_ZeroGrowth() {
        // Arrange
        LocalDate startDate = LocalDate.now().minusDays(2);
        LocalDate endDate = LocalDate.now();

        when(userRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);

        // Act
        List<DashboardStatsDTO.UserGrowthData> result = dashboardService.getUserGrowthData(startDate, endDate);

        // Assert
        assertEquals(3, result.size());
        result.forEach(data -> assertEquals(0, data.getCount()));
    }

    // ==================== getLatestUsers() 测试 ====================

    @Test
    void testGetLatestUsers_Success() {
        // Arrange
        List<User> users = Arrays.asList(testUser);
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(users));

        // Act
        List<DashboardStatsDTO.LatestUser> result = dashboardService.getLatestUsers();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        DashboardStatsDTO.LatestUser user = result.get(0);
        assertEquals(testUser.getId(), user.getId());
        assertEquals(testUser.getUsername(), user.getUsername());
        assertEquals(testUser.getEmail(), user.getEmail());
        assertEquals("USER", user.getRole()); // getValue() 返回简化格式
    }

    @Test
    void testGetLatestUsers_EmptyList() {
        // Arrange
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act
        List<DashboardStatsDTO.LatestUser> result = dashboardService.getLatestUsers();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ==================== 趋势计算测试 ====================

    @Test
    void testCalculateTrend_PositiveGrowth() {
        // 通过 getStatsCards 间接测试 calculateTrend
        when(userRepository.count()).thenReturn(110L);
        when(userRepository.countByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(100L);

        setupMinimalMocksForCafeteriaAndReview();

        DashboardStatsDTO.StatsCards result = dashboardService.getStatsCards();

        // 110 vs 100 = 10% 增长
        assertEquals(10.0, result.getUserTrend(), 0.1);
    }

    @Test
    void testCalculateTrend_NegativeGrowth() {
        when(userRepository.count()).thenReturn(90L);
        when(userRepository.countByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(100L);

        setupMinimalMocksForCafeteriaAndReview();

        DashboardStatsDTO.StatsCards result = dashboardService.getStatsCards();

        // 90 vs 100 = -10% 下降
        assertEquals(-10.0, result.getUserTrend(), 0.1);
    }

    @Test
    void testCalculateTrend_FromZero() {
        when(userRepository.count()).thenReturn(10L);
        when(userRepository.countByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(0L);

        setupMinimalMocksForCafeteriaAndReview();

        DashboardStatsDTO.StatsCards result = dashboardService.getStatsCards();

        // 从0开始应该是100%增长
        assertEquals(100.0, result.getUserTrend(), 0.1);
    }

    @Test
    void testCalculateTrend_BothZero() {
        when(userRepository.count()).thenReturn(0L);
        when(userRepository.countByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(0L);

        setupMinimalMocksForCafeteriaAndReview();

        DashboardStatsDTO.StatsCards result = dashboardService.getStatsCards();

        // 都为0应该是0%
        assertEquals(0.0, result.getUserTrend(), 0.1);
    }

    // ==================== 辅助方法 ====================

    private void setupMocksForSuccess() {
        // 用户数据
        when(userRepository.count()).thenReturn(100L);
        when(userRepository.countByCreatedAtBefore(any(LocalDateTime.class))).thenReturn(95L);
        when(userRepository.countByLastLoginAfter(any(LocalDateTime.class))).thenReturn(60L);
        when(userRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(10L);
        when(userRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(testUser)));

        // Cafeteria Service
        CafeteriaServiceClient.CafeteriaStatsResponse cafeteriaStats =
                CafeteriaServiceClient.CafeteriaStatsResponse.builder()
                        .totalCafeterias(10)
                        .yesterdayCafeterias(9)
                        .totalStalls(50)
                        .yesterdayStalls(48)
                        .build();
        when(cafeteriaServiceClient.getCafeteriaStats()).thenReturn(cafeteriaStats);

        // Review Service
        ReviewServiceClient.ReviewStatsResponse reviewStats =
                ReviewServiceClient.ReviewStatsResponse.builder()
                        .totalReviews(500)
                        .yesterdayReviews(480)
                        .todayReviews(30)
                        .yesterdayReviewsForToday(25)
                        .pendingComplaints(10)
                        .totalComplaints(100)
                        .build();
        when(reviewServiceClient.getReviewStats()).thenReturn(reviewStats);

        ReviewServiceClient.LatestReviewResponse reviewResponse =
                ReviewServiceClient.LatestReviewResponse.builder()
                        .id("review1")
                        .username("user1")
                        .stallName("Stall A")
                        .rating(5.0)
                        .createdAt(LocalDateTime.now())
                        .build();
        when(reviewServiceClient.getLatestReviews()).thenReturn(Arrays.asList(reviewResponse));
    }

    private void setupMinimalMocksForCafeteriaAndReview() {
        CafeteriaServiceClient.CafeteriaStatsResponse cafeteriaStats =
                CafeteriaServiceClient.CafeteriaStatsResponse.builder()
                        .totalCafeterias(0)
                        .yesterdayCafeterias(0)
                        .totalStalls(0)
                        .yesterdayStalls(0)
                        .build();
        when(cafeteriaServiceClient.getCafeteriaStats()).thenReturn(cafeteriaStats);

        ReviewServiceClient.ReviewStatsResponse reviewStats =
                ReviewServiceClient.ReviewStatsResponse.builder()
                        .totalReviews(0)
                        .yesterdayReviews(0)
                        .todayReviews(0)
                        .yesterdayReviewsForToday(0)
                        .build();
        when(reviewServiceClient.getReviewStats()).thenReturn(reviewStats);
    }
}
