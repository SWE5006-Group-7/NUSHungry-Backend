package com.nushungry.reviewservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.reviewservice.common.ApiResponse;
import com.nushungry.reviewservice.dto.*;
import com.nushungry.reviewservice.exception.ResourceNotFoundException;
import com.nushungry.reviewservice.exception.UnauthorizedException;
import com.nushungry.reviewservice.service.RatingCalculationService;
import com.nushungry.reviewservice.service.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import com.nushungry.reviewservice.util.JwtUtil;
import com.nushungry.reviewservice.filter.JwtAuthenticationFilter;

@WebMvcTest(
    controllers = {
        ReviewController.class,
        com.nushungry.reviewservice.exception.GlobalExceptionHandler.class
    },
    excludeAutoConfiguration = {
        MongoAutoConfiguration.class,
        MongoDataAutoConfiguration.class,
        RabbitAutoConfiguration.class,
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
    },
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {
                JwtAuthenticationFilter.class,
                com.nushungry.reviewservice.config.MongoConfig.class,
                com.nushungry.reviewservice.config.RabbitMQConfig.class,
                com.nushungry.reviewservice.config.SecurityConfig.class
            }
        )
    })
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private RatingCalculationService ratingCalculationService;

    private ReviewResponse reviewResponse;
    private CreateReviewRequest createRequest;
    private UpdateReviewRequest updateRequest;

    @BeforeEach
    void setUp() {
        reviewResponse = ReviewResponse.builder()
                .id("review123")
                .stallId(1L)
                .stallName("Test Stall")
                .userId("user123")
                .username("testuser")
                .rating(5)
                .comment("Great food!")
                .totalCost(15.0)
                .numberOfPeople(2)
                .likesCount(0)
                .isLikedByCurrentUser(false)
                .createdAt(LocalDateTime.now())
                .build();

        createRequest = CreateReviewRequest.builder()
                .stallId(1L)
                .stallName("Test Stall")
                .rating(5)
                .comment("Great food!")
                .totalCost(15.0)
                .numberOfPeople(2)
                .build();

        updateRequest = UpdateReviewRequest.builder()
                .rating(4)
                .comment("Updated comment")
                .totalCost(20.0)
                .numberOfPeople(2)
                .build();
    }

    @Test
    void createReview_Success() throws Exception {
        when(reviewService.createReview(any(CreateReviewRequest.class), anyString(), anyString(), anyString()))
                .thenReturn(reviewResponse);

        mockMvc.perform(post("/api/reviews")
                        .header("X-User-Id", "user123")
                        .header("X-Username", "testuser")
                        .header("X-User-Avatar", "avatar.jpg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Review created successfully"))
                .andExpect(jsonPath("$.data.id").value("review123"))
                .andExpect(jsonPath("$.data.stallId").value(1))
                .andExpect(jsonPath("$.data.rating").value(5));

        verify(reviewService, times(1)).createReview(any(CreateReviewRequest.class), eq("user123"), eq("testuser"), eq("avatar.jpg"));
    }

    @Test
    void createReview_InvalidRequest_MissingRating() throws Exception {
        CreateReviewRequest invalidRequest = CreateReviewRequest.builder()
                .stallId(1L)
                .comment("No rating")
                .build();

        mockMvc.perform(post("/api/reviews")
                        .header("X-User-Id", "user123")
                        .header("X-Username", "testuser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), anyString(), anyString(), anyString());
    }

    @Test
    void updateReview_Success() throws Exception {
        when(reviewService.updateReview(anyString(), any(UpdateReviewRequest.class), anyString()))
                .thenReturn(reviewResponse);

        mockMvc.perform(put("/api/reviews/review123")
                        .header("X-User-Id", "user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Review updated successfully"))
                .andExpect(jsonPath("$.data.id").value("review123"));

        verify(reviewService, times(1)).updateReview(eq("review123"), any(UpdateReviewRequest.class), eq("user123"));
    }

    @Test
    void updateReview_NotFound() throws Exception {
        when(reviewService.updateReview(anyString(), any(UpdateReviewRequest.class), anyString()))
                .thenThrow(new ResourceNotFoundException("Review not found"));

        mockMvc.perform(put("/api/reviews/nonexistent")
                        .header("X-User-Id", "user123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isNotFound());

        verify(reviewService, times(1)).updateReview(eq("nonexistent"), any(UpdateReviewRequest.class), eq("user123"));
    }

    @Test
    void updateReview_Unauthorized() throws Exception {
        when(reviewService.updateReview(anyString(), any(UpdateReviewRequest.class), anyString()))
                .thenThrow(new UnauthorizedException("You can only update your own reviews"));

        mockMvc.perform(put("/api/reviews/review123")
                        .header("X-User-Id", "otherUser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isForbidden());

        verify(reviewService, times(1)).updateReview(eq("review123"), any(UpdateReviewRequest.class), eq("otherUser"));
    }

    @Test
    void deleteReview_Success() throws Exception {
        doNothing().when(reviewService).deleteReview(anyString(), anyString());

        mockMvc.perform(delete("/api/reviews/review123")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Review deleted successfully"));

        verify(reviewService, times(1)).deleteReview(eq("review123"), eq("user123"));
    }

    @Test
    void deleteReview_Unauthorized() throws Exception {
        doThrow(new UnauthorizedException("You can only delete your own reviews"))
                .when(reviewService).deleteReview(anyString(), anyString());

        mockMvc.perform(delete("/api/reviews/review123")
                        .header("X-User-Id", "otherUser"))
                .andExpect(status().isForbidden());

        verify(reviewService, times(1)).deleteReview(eq("review123"), eq("otherUser"));
    }

    @Test
    void getReviewById_Success() throws Exception {
        when(reviewService.getReviewById(anyString(), anyString()))
                .thenReturn(reviewResponse);

        mockMvc.perform(get("/api/reviews/review123")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("review123"))
                .andExpect(jsonPath("$.data.stallId").value(1));

        verify(reviewService, times(1)).getReviewById(eq("review123"), eq("user123"));
    }

    @Test
    void getReviewById_NotFound() throws Exception {
        when(reviewService.getReviewById(anyString(), anyString()))
                .thenThrow(new ResourceNotFoundException("Review not found"));

        mockMvc.perform(get("/api/reviews/nonexistent")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isNotFound());

        verify(reviewService, times(1)).getReviewById(eq("nonexistent"), eq("user123"));
    }

    @Test
    void getReviewsByStallId_DefaultSort() throws Exception {
        List<ReviewResponse> reviews = Arrays.asList(reviewResponse);
        Page<ReviewResponse> page = new PageImpl<>(reviews, PageRequest.of(0, 10), 1);

        when(reviewService.getReviewsByStallId(anyLong(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/reviews/stall/1")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value("review123"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(reviewService, times(1)).getReviewsByStallId(eq(1L), eq("createdAt"), eq("user123"), any(Pageable.class));
    }

    @Test
    void getReviewsByStallId_SortByLikes() throws Exception {
        List<ReviewResponse> reviews = Arrays.asList(reviewResponse);
        Page<ReviewResponse> page = new PageImpl<>(reviews, PageRequest.of(0, 10), 1);

        when(reviewService.getReviewsByStallId(anyLong(), anyString(), anyString(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/reviews/stall/1")
                        .param("sortBy", "likesCount")
                        .param("page", "0")
                        .param("size", "10")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value("review123"));

        verify(reviewService, times(1)).getReviewsByStallId(eq(1L), eq("likesCount"), eq("user123"), any(Pageable.class));
    }

    @Test
    void getReviewsByUserId_Success() throws Exception {
        List<ReviewResponse> reviews = Arrays.asList(reviewResponse);
        Page<ReviewResponse> page = new PageImpl<>(reviews, PageRequest.of(0, 10), 1);

        when(reviewService.getReviewsByUserId(anyString(), anyString(), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/reviews/user/user123")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value("review123"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(reviewService, times(1)).getReviewsByUserId(eq("user123"), eq("user123"), any(Pageable.class));
    }

    @Test
    void getRatingDistribution_Success() throws Exception {
        Map<Integer, Long> distribution = new HashMap<>();
        distribution.put(5, 10L);
        distribution.put(4, 5L);
        distribution.put(3, 2L);
        distribution.put(2, 1L);
        distribution.put(1, 0L);

        RatingDistributionResponse response = RatingDistributionResponse.builder()
                .stallId(1L)
                .averageRating(4.5)
                .totalReviews(18L)
                .distribution(distribution)
                .build();

        when(ratingCalculationService.getRatingDistribution(anyLong()))
                .thenReturn(response);

        mockMvc.perform(get("/api/reviews/stall/1/rating-distribution"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stallId").value(1))
                .andExpect(jsonPath("$.data.averageRating").value(4.5))
                .andExpect(jsonPath("$.data.totalReviews").value(18))
                .andExpect(jsonPath("$.data.distribution.5").value(10));

        verify(ratingCalculationService, times(1)).getRatingDistribution(eq(1L));
    }

    @Test
    void createReview_MissingHeaders() throws Exception {
        // GlobalExceptionHandler 会捕获 MissingRequestHeaderException 并返回 400
        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Missing required header")));

        verify(reviewService, never()).createReview(any(), anyString(), anyString(), anyString());
    }

    @Test
    void createReview_InvalidRating_TooHigh() throws Exception {
        CreateReviewRequest invalidRequest = CreateReviewRequest.builder()
                .stallId(1L)
                .stallName("Test Stall")
                .rating(6)  // 超出范围 (最大5)
                .comment("Test comment")
                .build();

        mockMvc.perform(post("/api/reviews")
                        .header("X-User-Id", "user123")
                        .header("X-Username", "testuser")
                        .header("X-User-Avatar", "avatar.jpg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), anyString(), anyString(), anyString());
    }

    @Test
    void createReview_InvalidRating_TooLow() throws Exception {
        CreateReviewRequest invalidRequest = CreateReviewRequest.builder()
                .stallId(1L)
                .stallName("Test Stall")
                .rating(0)  // 超出范围 (最小1)
                .comment("Test comment")
                .build();

        mockMvc.perform(post("/api/reviews")
                        .header("X-User-Id", "user123")
                        .header("X-Username", "testuser")
                        .header("X-User-Avatar", "avatar.jpg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), anyString(), anyString(), anyString());
    }

    @Test
    void createReview_InvalidComment_TooLong() throws Exception {
        // 创建超过1000字符的评论
        String longComment = "a".repeat(1001);

        CreateReviewRequest invalidRequest = CreateReviewRequest.builder()
                .stallId(1L)
                .stallName("Test Stall")
                .rating(5)
                .comment(longComment)
                .build();

        mockMvc.perform(post("/api/reviews")
                        .header("X-User-Id", "user123")
                        .header("X-Username", "testuser")
                        .header("X-User-Avatar", "avatar.jpg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), anyString(), anyString(), anyString());
    }

    @Test
    void createReview_InvalidComment_Blank() throws Exception {
        CreateReviewRequest invalidRequest = CreateReviewRequest.builder()
                .stallId(1L)
                .stallName("Test Stall")
                .rating(5)
                .comment("   ")  // 空白评论
                .build();

        mockMvc.perform(post("/api/reviews")
                        .header("X-User-Id", "user123")
                        .header("X-Username", "testuser")
                        .header("X-User-Avatar", "avatar.jpg")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).createReview(any(), anyString(), anyString(), anyString());
    }
}
