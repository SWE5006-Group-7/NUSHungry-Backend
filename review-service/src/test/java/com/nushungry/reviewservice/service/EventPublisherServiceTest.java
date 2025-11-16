package com.nushungry.reviewservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.reviewservice.event.PriceChangedEvent;
import com.nushungry.reviewservice.event.RatingChangedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class EventPublisherServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private EventPublisherService eventPublisherService;

    // 使用真实的 ObjectMapper 而不是 Mock,用于 JSON 序列化测试
    private final ObjectMapper objectMapper;

    {
        // 注册 JavaTimeModule 以支持 LocalDateTime 序列化
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
    }

    private RatingChangedEvent ratingChangedEvent;
    private PriceChangedEvent priceChangedEvent;

    @BeforeEach
    void setUp() {
        // 使用 ReflectionTestUtils 注入 @Value 配置 - 字段名必须与实际代码一致
        ReflectionTestUtils.setField(eventPublisherService, "reviewExchange", "review.exchange");
        ReflectionTestUtils.setField(eventPublisherService, "ratingChangedRoutingKey", "review.rating.changed");
        ReflectionTestUtils.setField(eventPublisherService, "priceChangedRoutingKey", "review.price.changed");

        ratingChangedEvent = RatingChangedEvent.builder()
                .stallId(1L)
                .newAverageRating(4.5)
                .reviewCount(10L)
                .timestamp(LocalDateTime.now())
                .build();

        priceChangedEvent = PriceChangedEvent.builder()
                .stallId(1L)
                .newAveragePrice(15.5)
                .priceCount(10L)
                .timestamp(LocalDateTime.now())
                .build();
    }


    @Test
    void testPublishRatingChanged_Success() {
        // When
        eventPublisherService.publishRatingChanged(ratingChangedEvent);

        // Then
        verify(rabbitTemplate, times(1)).convertAndSend(
                anyString(),
                anyString(),
                any(RatingChangedEvent.class)
        );
    }


    @Test
    void testPublishPriceChanged_Success() {
        // When
        eventPublisherService.publishPriceChanged(priceChangedEvent);

        // Then
        verify(rabbitTemplate, times(1)).convertAndSend(
                anyString(),
                anyString(),
                any(PriceChangedEvent.class)
        );
    }


    @Test
    void testRatingChangedEvent_JsonFormat() throws Exception {
        // When
        String json = objectMapper.writeValueAsString(ratingChangedEvent);

        // Then
        assert json.contains("\"stallId\":1");
        assert json.contains("\"newAverageRating\":4.5");
        assert json.contains("\"reviewCount\":10");
        assert json.contains("\"timestamp\"");

        // Verify can deserialize
        RatingChangedEvent deserialized = objectMapper.readValue(json, RatingChangedEvent.class);
        assert deserialized.getStallId().equals(1L);
        assert deserialized.getNewAverageRating().equals(4.5);
        assert deserialized.getReviewCount().equals(10L);
    }


    @Test
    void testPriceChangedEvent_JsonFormat() throws Exception {
        // When
        String json = objectMapper.writeValueAsString(priceChangedEvent);

        // Then
        assert json.contains("\"stallId\":1");
        assert json.contains("\"newAveragePrice\":15.5");
        assert json.contains("\"priceCount\":10");
        assert json.contains("\"timestamp\"");

        // Verify can deserialize
        PriceChangedEvent deserialized = objectMapper.readValue(json, PriceChangedEvent.class);
        assert deserialized.getStallId().equals(1L);
        assert deserialized.getNewAveragePrice().equals(15.5);
        assert deserialized.getPriceCount().equals(10L);
    }


    @Test
    void testPublishRatingChanged_RabbitMQFailure() {
        // Given
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When - should not throw exception (handled internally)
        eventPublisherService.publishRatingChanged(ratingChangedEvent);

        // Then - verify method was called and exception was caught
        verify(rabbitTemplate, times(1)).convertAndSend(
                anyString(),
                anyString(),
                any(Object.class)
        );
    }


    @Test
    void testPublishPriceChanged_RabbitMQFailure() {
        // Given
        doThrow(new RuntimeException("RabbitMQ connection failed"))
                .when(rabbitTemplate).convertAndSend(anyString(), anyString(), any(Object.class));

        // When - should not throw exception (handled internally)
        eventPublisherService.publishPriceChanged(priceChangedEvent);

        // Then - verify method was called and exception was caught
        verify(rabbitTemplate, times(1)).convertAndSend(
                anyString(),
                anyString(),
                any(Object.class)
        );
    }


    @Test
    void testPublishRatingChanged_CorrectRoutingKey() {
        // When
        eventPublisherService.publishRatingChanged(ratingChangedEvent);

        // Then - verify correct routing key is used
        verify(rabbitTemplate).convertAndSend(
                "review.exchange",
                "review.rating.changed",
                ratingChangedEvent
        );
    }


    @Test
    void testPublishPriceChanged_CorrectRoutingKey() {
        // When
        eventPublisherService.publishPriceChanged(priceChangedEvent);

        // Then - verify correct routing key is used
        verify(rabbitTemplate).convertAndSend(
                "review.exchange",
                "review.price.changed",
                priceChangedEvent
        );
    }


    @Test
    void testPublishRatingChanged_NullStallId() {
        // Given
        RatingChangedEvent nullEvent = RatingChangedEvent.builder()
                .stallId(null)
                .newAverageRating(4.5)
                .reviewCount(10L)
                .timestamp(LocalDateTime.now())
                .build();

        // When
        eventPublisherService.publishRatingChanged(nullEvent);

        // Then - should still attempt to publish
        verify(rabbitTemplate, times(1)).convertAndSend(
                anyString(),
                anyString(),
                any(RatingChangedEvent.class)
        );
    }
}
