package com.nushungry.userservice.service;

import com.nushungry.userservice.client.CafeteriaServiceClient;
import com.nushungry.userservice.client.ReviewServiceClient;
import com.nushungry.userservice.dto.DashboardStatsDTO;
import com.nushungry.userservice.model.User;
import com.nushungry.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserRepository userRepository;
    private final CafeteriaServiceClient cafeteriaServiceClient;
    private final ReviewServiceClient reviewServiceClient;

    // 系统启动时间（假设系统从30天前开始运行）
    private static final LocalDateTime SYSTEM_START_TIME = LocalDateTime.now().minusDays(30);

    public DashboardStatsDTO getDashboardStats() {
        log.info("获取仪表板统计数据");
        return DashboardStatsDTO.builder()
                .statsCards(getStatsCards())
                .systemOverview(getSystemOverview())
                .userGrowthData(getUserGrowthData())
                .latestUsers(getLatestUsers())
                .latestReviews(getLatestReviews())
                .build();
    }

    public DashboardStatsDTO.StatsCards getStatsCards() {
        log.info("获取统计卡片数据");

        // 获取本地用户数据
        long totalUsers = userRepository.count();
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        long yesterdayUsers = userRepository.countByCreatedAtBefore(yesterday);

        // 调用Cafeteria Service获取食堂和档口数据
        CafeteriaServiceClient.CafeteriaStatsResponse cafeteriaStats = null;
        try {
            cafeteriaStats = cafeteriaServiceClient.getCafeteriaStats();
            log.info("成功获取食堂服务统计数据");
        } catch (Exception e) {
            log.error("调用Cafeteria Service失败", e);
            cafeteriaStats = CafeteriaServiceClient.CafeteriaStatsResponse.builder()
                    .totalCafeterias(0)
                    .yesterdayCafeterias(0)
                    .totalStalls(0)
                    .yesterdayStalls(0)
                    .build();
        }

        // 调用Review Service获取评价数据
        ReviewServiceClient.ReviewStatsResponse reviewStats = null;
        try {
            reviewStats = reviewServiceClient.getReviewStats();
            log.info("成功获取评价服务统计数据");
        } catch (Exception e) {
            log.error("调用Review Service失败", e);
            reviewStats = ReviewServiceClient.ReviewStatsResponse.builder()
                    .totalReviews(0)
                    .yesterdayReviews(0)
                    .todayReviews(0)
                    .yesterdayReviewsForToday(0)
                    .build();
        }

        return DashboardStatsDTO.StatsCards.builder()
                .totalUsers((int) totalUsers)
                .userTrend(calculateTrend(totalUsers, yesterdayUsers))
                .totalCafeterias(cafeteriaStats.getTotalCafeterias())
                .cafeteriaTrend(calculateTrend(cafeteriaStats.getTotalCafeterias(), cafeteriaStats.getYesterdayCafeterias()))
                .totalStalls(cafeteriaStats.getTotalStalls())
                .stallTrend(calculateTrend(cafeteriaStats.getTotalStalls(), cafeteriaStats.getYesterdayStalls()))
                .totalReviews(reviewStats.getTotalReviews())
                .reviewTrend(calculateTrend(reviewStats.getTotalReviews(), reviewStats.getYesterdayReviews()))
                .todayOrders(reviewStats.getTodayReviews())
                .orderTrend(calculateTrend(reviewStats.getTodayReviews(), reviewStats.getYesterdayReviewsForToday()))
                .build();
    }

    public DashboardStatsDTO.SystemOverview getSystemOverview() {
        log.info("获取系统概览");

        // 计算系统运行天数
        long runningDays = ChronoUnit.DAYS.between(SYSTEM_START_TIME, LocalDateTime.now());

        // 计算活跃用户（最近7天有登录的用户）
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long activeUsers = userRepository.countByLastLoginAfter(sevenDaysAgo);
        long totalUsers = userRepository.count();
        double activePercentage = totalUsers > 0 ? (activeUsers * 100.0 / totalUsers) : 0;

        // 调用Review Service获取投诉数据
        int pendingComplaints = 0;
        double pendingPercentage = 0;
        try {
            ReviewServiceClient.ReviewStatsResponse reviewStats = reviewServiceClient.getReviewStats();
            pendingComplaints = reviewStats.getPendingComplaints();
            int totalComplaints = reviewStats.getTotalComplaints();
            pendingPercentage = totalComplaints > 0 ? (pendingComplaints * 100.0 / totalComplaints) : 0;
            log.info("成功获取投诉数据");
        } catch (Exception e) {
            log.error("调用Review Service获取投诉数据失败", e);
        }

        // 计算系统健康度
        int healthScore = calculateHealthScore(activePercentage, pendingPercentage);
        String healthStatus = getHealthStatus(healthScore);

        return DashboardStatsDTO.SystemOverview.builder()
                .runningDays(runningDays)
                .activeUsers((int) activeUsers)
                .activeUserPercentage(activePercentage)
                .pendingComplaints(pendingComplaints)
                .pendingComplaintPercentage(pendingPercentage)
                .healthScore(healthScore)
                .healthStatus(healthStatus)
                .build();
    }

    private List<DashboardStatsDTO.UserGrowthData> getUserGrowthData() {
        // 默认返回最近7天的数据
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        return getUserGrowthData(startDate, endDate);
    }

    public List<DashboardStatsDTO.UserGrowthData> getUserGrowthData(LocalDate startDate, LocalDate endDate) {
        log.info("获取用户增长数据: {} 到 {}", startDate, endDate);
        List<DashboardStatsDTO.UserGrowthData> growthData = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 遍历日期范围,统计每天的新增用户数
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            LocalDateTime startOfDay = currentDate.atStartOfDay();
            LocalDateTime endOfDay = currentDate.plusDays(1).atStartOfDay();

            long count = userRepository.countByCreatedAtBetween(startOfDay, endOfDay);

            growthData.add(DashboardStatsDTO.UserGrowthData.builder()
                    .date(currentDate.format(formatter))
                    .count((int) count)
                    .build());

            currentDate = currentDate.plusDays(1);
        }

        return growthData;
    }

    public List<DashboardStatsDTO.LatestUser> getLatestUsers() {
        log.info("获取最新用户列表");
        return userRepository.findAll(
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
                ).stream()
                .map(user -> DashboardStatsDTO.LatestUser.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .role(user.getRole().getValue())
                        .avatarUrl(user.getAvatarUrl())
                        .enabled(user.getEnabled())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private List<DashboardStatsDTO.LatestReview> getLatestReviews() {
        log.info("获取最新评价列表");
        try {
            List<ReviewServiceClient.LatestReviewResponse> reviews = reviewServiceClient.getLatestReviews();
            return reviews.stream()
                    .map(review -> DashboardStatsDTO.LatestReview.builder()
                            .id(review.getId())
                            .username(review.getUsername())
                            .stallName(review.getStallName())
                            .rating(review.getRating())
                            .createdAt(review.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("调用Review Service获取最新评价失败", e);
            return new ArrayList<>();
        }
    }

    private double calculateTrend(long current, long previous) {
        if (previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((current - previous) * 100.0) / previous;
    }

    private int calculateHealthScore(double activePercentage, double pendingPercentage) {
        // 基础分数
        int score = 100;

        // 根据活跃用户百分比调整
        if (activePercentage < 30) {
            score -= 20;
        } else if (activePercentage < 50) {
            score -= 10;
        }

        // 根据待处理投诉百分比调整
        if (pendingPercentage > 50) {
            score -= 30;
        } else if (pendingPercentage > 30) {
            score -= 15;
        }

        return Math.max(0, Math.min(100, score));
    }

    private String getHealthStatus(int healthScore) {
        if (healthScore >= 90) {
            return "优秀";
        } else if (healthScore >= 70) {
            return "良好";
        } else if (healthScore >= 50) {
            return "一般";
        } else {
            return "需要关注";
        }
    }
}
