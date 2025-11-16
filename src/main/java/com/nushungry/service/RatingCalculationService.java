package com.nushungry.service;

import com.nushungry.model.Stall;
import com.nushungry.repository.ReviewRepository;
import com.nushungry.repository.StallRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 评分计算服务
 * 负责计算和更新摊位的平均评分和评价数量
 */
@Slf4j
@Service
public class RatingCalculationService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private StallRepository stallRepository;

    /**
     * 重新计算并更新摊位的评分统计信息
     * @param stallId 摊位ID
     */
    @Transactional
    public void recalculateStallRating(Long stallId) {
        log.info("Recalculating rating for stall: {}", stallId);

        // 查找摊位
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new RuntimeException("Stall not found with id: " + stallId));

        // 计算平均评分
        Double averageRating = reviewRepository.getAverageRatingByStallId(stallId);
        if (averageRating == null) {
            averageRating = 0.0;
        }

        // 统计评价数量
        long reviewCount = reviewRepository.countByStallId(stallId);

        // 更新摊位信息
        stall.setAverageRating(Math.round(averageRating * 10.0) / 10.0); // 保留一位小数
        stall.setReviewCount((int) reviewCount);
        stallRepository.save(stall);

        log.info("Updated stall {} rating: {} (count: {})", stallId, stall.getAverageRating(), reviewCount);
    }

    /**
     * 批量重新计算所有摊位的评分
     */
    @Transactional
    public void recalculateAllStallRatings() {
        log.info("Recalculating ratings for all stalls");
        stallRepository.findAll().forEach(stall -> {
            try {
                recalculateStallRating(stall.getId());
            } catch (Exception e) {
                log.error("Error calculating rating for stall {}: {}", stall.getId(), e.getMessage());
            }
        });
        log.info("Finished recalculating all stall ratings");
    }
}
