package com.nushungry.service;

import com.nushungry.dto.DashboardStatsDTO;
import com.nushungry.model.Review;
import com.nushungry.model.Stall;
import com.nushungry.model.User;
import com.nushungry.repository.ReviewRepository;
import com.nushungry.repository.StallRepository;
import com.nushungry.repository.UserRepository;
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
    private final StallRepository stallRepository;
    private final ReviewRepository reviewRepository;

    // 系统启动时间（假设系统从30天前开始运行）
    private static final LocalDateTime SYSTEM_START_TIME = LocalDateTime.now().minusDays(30);

    public DashboardStatsDTO getDashboardStats() {
        return DashboardStatsDTO.builder()
                .statsCards(getStatsCards())
                .systemOverview(getSystemOverview())
                .userGrowthData(getUserGrowthData())
                .latestUsers(getLatestUsers())
                .latestReviews(getLatestReviews())
                .build();
    }

    private DashboardStatsDTO.StatsCards getStatsCards() {
        // 获取总数
        long totalUsers = userRepository.count();
        long totalStalls = stallRepository.count();
        long totalReviews = reviewRepository.count();

        // 获取昨天的数据来计算趋势
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        long yesterdayUsers = userRepository.countByCreatedAtBefore(yesterday);
        long yesterdayStalls = stallRepository.countByCreatedAtBefore(yesterday);
        long yesterdayReviews = reviewRepository.countByCreatedAtBefore(yesterday);

        // 计算今日订单（这里暂时使用今日评价数代替）
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        long todayOrders = reviewRepository.countByCreatedAtAfter(todayStart);
        long yesterdayOrders = reviewRepository.countByCreatedAtBetween(
                todayStart.minusDays(1),
                todayStart
        );

        return DashboardStatsDTO.StatsCards.builder()
                .totalUsers((int) totalUsers)
                .userTrend(calculateTrend(totalUsers, yesterdayUsers))
                .totalStalls((int) totalStalls)
                .stallTrend(calculateTrend(totalStalls, yesterdayStalls))
                .totalReviews((int) totalReviews)
                .reviewTrend(calculateTrend(totalReviews, yesterdayReviews))
                .todayOrders((int) todayOrders)
                .orderTrend(calculateTrend(todayOrders, yesterdayOrders))
                .build();
    }

    private DashboardStatsDTO.SystemOverview getSystemOverview() {
        // 计算系统运行天数
        long runningDays = ChronoUnit.DAYS.between(SYSTEM_START_TIME, LocalDateTime.now());

        // 计算活跃用户（最近7天有登录的用户）
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        long activeUsers = userRepository.countByLastLoginAfter(sevenDaysAgo);
        long totalUsers = userRepository.count();
        double activePercentage = totalUsers > 0 ? (activeUsers * 100.0 / totalUsers) : 0;

        // 待处理投诉（暂时使用低分评价数代替）
        long pendingComplaints = reviewRepository.countByRatingLessThanAndProcessedFalse(3.0);
        long totalComplaints = reviewRepository.countByRatingLessThan(3.0);
        double pendingPercentage = totalComplaints > 0 ? (pendingComplaints * 100.0 / totalComplaints) : 0;

        // 计算系统健康度
        int healthScore = calculateHealthScore(activePercentage, pendingPercentage);
        String healthStatus = getHealthStatus(healthScore);

        return DashboardStatsDTO.SystemOverview.builder()
                .runningDays(runningDays)
                .activeUsers((int) activeUsers)
                .activeUserPercentage(activePercentage)
                .pendingComplaints((int) pendingComplaints)
                .pendingComplaintPercentage(pendingPercentage)
                .healthScore(healthScore)
                .healthStatus(healthStatus)
                .build();
    }

    private List<DashboardStatsDTO.UserGrowthData> getUserGrowthData() {
        List<DashboardStatsDTO.UserGrowthData> growthData = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        // 获取最近7天的用户增长数据
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.plusDays(1).atStartOfDay();

            long count = userRepository.countByCreatedAtBetween(startOfDay, endOfDay);

            growthData.add(DashboardStatsDTO.UserGrowthData.builder()
                    .date(date.format(formatter))
                    .count((int) count)
                    .build());
        }

        return growthData;
    }

    private List<DashboardStatsDTO.LatestUser> getLatestUsers() {
        return userRepository.findAll(
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
                ).stream()
                .map(user -> DashboardStatsDTO.LatestUser.builder()
                        .id(user.getId())
                        .username(user.getUsername())
                        .email(user.getEmail())
                        .enabled(user.isEnabled())
                        .createdAt(user.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private List<DashboardStatsDTO.LatestReview> getLatestReviews() {
        return reviewRepository.findAll(
                        PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
                ).stream()
                .map(review -> {
                    String username = review.getUser() != null ? review.getUser().getUsername() : "Unknown";
                    String stallName = review.getStall() != null ? review.getStall().getName() : "Unknown";

                    return DashboardStatsDTO.LatestReview.builder()
                            .id(review.getId())
                            .username(username)
                            .stallName(stallName)
                            .rating(review.getRating())
                            .createdAt(review.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
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