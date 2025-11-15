package com.nushungry.userservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * ReviewServiceClient 降级处理
 */
@Slf4j
@Component
public class ReviewServiceClientFallback implements ReviewServiceClient {

    @Override
    public ReviewStatsResponse getReviewStats() {
        log.warn("ReviewService调用失败，使用降级数据");
        return ReviewStatsResponse.builder()
                .totalReviews(0)
                .yesterdayReviews(0)
                .todayReviews(0)
                .yesterdayReviewsForToday(0)
                .pendingComplaints(0)
                .totalComplaints(0)
                .build();
    }

    @Override
    public List<LatestReviewResponse> getLatestReviews() {
        log.warn("ReviewService调用失败，返回空列表");
        return Collections.emptyList();
    }
}
