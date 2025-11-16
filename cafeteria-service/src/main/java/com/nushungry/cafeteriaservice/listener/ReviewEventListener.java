package com.nushungry.cafeteriaservice.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.cafeteriaservice.config.RabbitMQConfig;
import com.nushungry.cafeteriaservice.model.Stall;
import com.nushungry.cafeteriaservice.repository.StallRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ReviewEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ReviewEventListener.class);

    private final StallRepository stallRepository;
    private final ObjectMapper objectMapper;

    public ReviewEventListener(StallRepository stallRepository) {
        this.stallRepository = stallRepository;
        this.objectMapper = new ObjectMapper();
    }

    @RabbitListener(queues = RabbitMQConfig.REVIEW_EVENT_QUEUE)
    public void handleReviewEvent(String message) {
        logger.info("Received review event: {}", message);
        try {
            // 解析 RatingChangedEvent 格式: {"stallId":1, "newAverageRating":4.3, "reviewCount":10, "timestamp":"..."}
            Map<String, Object> event = objectMapper.readValue(message, Map.class);

            Long stallId = getLongValue(event, "stallId");
            Double newAverageRating = getDoubleValue(event, "newAverageRating");
            Long reviewCount = getLongValue(event, "reviewCount");

            if (stallId == null) {
                logger.warn("Missing stallId in event: {}", message);
                return;
            }

            stallRepository.findById(stallId).ifPresent(stall -> {
                if (newAverageRating != null) {
                    stall.setAvgRating(newAverageRating);
                    logger.info("Updated stall {} avgRating to {}", stallId, newAverageRating);
                }
                if (reviewCount != null) {
                    stall.setReviewCount(reviewCount.intValue());
                    logger.info("Updated stall {} review count to {}", stallId, reviewCount);
                }
                stallRepository.save(stall);
                logger.info("Successfully updated stall {} from rating event", stallId);
            });
        } catch (Exception ex) {
            logger.error("Failed to process review event: {}", message, ex);
        }
    }

    @RabbitListener(queues = RabbitMQConfig.PRICE_EVENT_QUEUE)
    public void handlePriceEvent(String message) {
        logger.info("Received price event: {}", message);
        try {
            // 解析 PriceChangedEvent 格式: {"stallId":1, "newAveragePrice":12.5, "priceCount":10, "timestamp":"..."}
            Map<String, Object> event = objectMapper.readValue(message, Map.class);

            Long stallId = getLongValue(event, "stallId");
            Double newAveragePrice = getDoubleValue(event, "newAveragePrice");

            if (stallId == null) {
                logger.warn("Missing stallId in price event: {}", message);
                return;
            }

            stallRepository.findById(stallId).ifPresent(stall -> {
                if (newAveragePrice != null) {
                    stall.setAveragePrice(newAveragePrice);
                    logger.info("Updated stall {} averagePrice to {}", stallId, newAveragePrice);
                    stallRepository.save(stall);
                    logger.info("Successfully updated stall {} from price event", stallId);
                } else {
                    logger.warn("No averagePrice in event for stall {}", stallId);
                }
            });
        } catch (Exception ex) {
            logger.error("Failed to process price event: {}", message, ex);
        }
    }

    private Long getLongValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
}


