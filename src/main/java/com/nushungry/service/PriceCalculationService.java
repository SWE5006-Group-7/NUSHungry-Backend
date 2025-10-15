package com.nushungry.service;

import com.nushungry.model.Review;
import com.nushungry.model.Stall;
import com.nushungry.repository.ReviewRepository;
import com.nushungry.repository.StallRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 价格计算服务
 * 负责计算和更新摊位的人均价格
 */
@Slf4j
@Service
public class PriceCalculationService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private StallRepository stallRepository;

    /**
     * 重新计算并更新摊位的人均价格
     * @param stallId 摊位ID
     */
    @Transactional
    public void recalculateStallAveragePrice(Long stallId) {
        log.info("Recalculating average price for stall: {}", stallId);

        // 查找摊位
        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new RuntimeException("Stall not found with id: " + stallId));

        // 获取所有包含花费信息的评价
        List<Review> reviewsWithCost = reviewRepository.findAll().stream()
                .filter(r -> r.getStall() != null && r.getStall().getId().equals(stallId))
                .filter(r -> r.getTotalCost() != null && r.getTotalCost() > 0)
                .filter(r -> r.getNumberOfPeople() != null && r.getNumberOfPeople() > 0)
                .toList();

        if (reviewsWithCost.isEmpty()) {
            // 如果没有包含花费信息的评价，将人均价格设为0
            stall.setAveragePrice(0.0);
            stallRepository.save(stall);
            log.info("No reviews with cost info for stall {}, set averagePrice to 0", stallId);
            return;
        }

        // 计算每条评价的人均价格并求平均值
        double totalPerCapitaPrice = reviewsWithCost.stream()
                .mapToDouble(review -> review.getTotalCost() / review.getNumberOfPeople())
                .sum();

        double averagePrice = totalPerCapitaPrice / reviewsWithCost.size();

        // 更新摊位信息（保留两位小数）
        stall.setAveragePrice(Math.round(averagePrice * 100.0) / 100.0);
        stallRepository.save(stall);

        log.info("Updated stall {} average price: ${} (based on {} reviews)",
                stallId, stall.getAveragePrice(), reviewsWithCost.size());
    }

    /**
     * 批量重新计算所有摊位的人均价格
     */
    @Transactional
    public void recalculateAllStallPrices() {
        log.info("Recalculating average prices for all stalls");
        stallRepository.findAll().forEach(stall -> {
            try {
                recalculateStallAveragePrice(stall.getId());
            } catch (Exception e) {
                log.error("Error calculating average price for stall {}: {}", stall.getId(), e.getMessage());
            }
        });
        log.info("Finished recalculating all stall average prices");
    }
}
