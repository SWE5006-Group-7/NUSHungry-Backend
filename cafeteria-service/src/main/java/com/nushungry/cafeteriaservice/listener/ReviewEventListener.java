package com.nushungry.cafeteriaservice.listener;

import com.nushungry.cafeteriaservice.config.RabbitMQConfig;
import com.nushungry.cafeteriaservice.model.Stall;
import com.nushungry.cafeteriaservice.repository.StallRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class ReviewEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ReviewEventListener.class);

    private final StallRepository stallRepository;

    public ReviewEventListener(StallRepository stallRepository) {
        this.stallRepository = stallRepository;
    }

    @RabbitListener(queues = RabbitMQConfig.REVIEW_EVENT_QUEUE)
    public void handleReviewEvent(String message) {
        logger.info("Received review event: {}", message);
        try {
            // 期待格式: {"stallId":1, "avgRating":4.3, "avgPrice":6.5}
            var json = new org.json.JSONObject(message);
            long stallId = json.getLong("stallId");
            Double avgRating = json.has("avgRating") && !json.isNull("avgRating") ? json.getDouble("avgRating") : null;
            Double avgPrice = json.has("avgPrice") && !json.isNull("avgPrice") ? json.getDouble("avgPrice") : null;

            stallRepository.findById(stallId).ifPresent(stall -> {
                if (avgRating != null) {
                    stall.setAvgRating(avgRating);
                }
                if (avgPrice != null) {
                    stall.setAvgPrice(avgPrice);
                }
                stallRepository.save(stall);
                logger.info("Updated stall {} with avgRating={}, avgPrice={}", stallId, avgRating, avgPrice);
            });
        } catch (Exception ex) {
            logger.warn("Failed to process review event: {}", message, ex);
        }
    }
}


