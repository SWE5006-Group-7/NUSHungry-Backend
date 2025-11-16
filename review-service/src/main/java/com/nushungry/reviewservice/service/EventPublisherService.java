package com.nushungry.reviewservice.service;

import com.nushungry.reviewservice.event.PriceChangedEvent;
import com.nushungry.reviewservice.event.RatingChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventPublisherService {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.review}")
    private String reviewExchange;

    @Value("${rabbitmq.routing-key.rating-changed}")
    private String ratingChangedRoutingKey;

    @Value("${rabbitmq.routing-key.price-changed}")
    private String priceChangedRoutingKey;

    public void publishRatingChanged(RatingChangedEvent event) {
        try {
            log.info("Publishing rating changed event for stall ID: {}", event.getStallId());
            rabbitTemplate.convertAndSend(reviewExchange, ratingChangedRoutingKey, event);
            log.info("Successfully published rating changed event");
        } catch (Exception e) {
            log.error("Failed to publish rating changed event", e);
        }
    }

    public void publishPriceChanged(PriceChangedEvent event) {
        try {
            log.info("Publishing price changed event for stall ID: {}", event.getStallId());
            rabbitTemplate.convertAndSend(reviewExchange, priceChangedRoutingKey, event);
            log.info("Successfully published price changed event");
        } catch (Exception e) {
            log.error("Failed to publish price changed event", e);
        }
    }
}
