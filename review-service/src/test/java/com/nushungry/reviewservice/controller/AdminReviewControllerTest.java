package com.nushungry.reviewservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.reviewservice.document.ReviewDocument;
import com.nushungry.reviewservice.dto.ReviewStatsResponse;
import com.nushungry.reviewservice.repository.ReviewRepository;
import com.nushungry.reviewservice.service.ReviewService;
import com.nushungry.reviewservice.service.RatingCalculationService;
import com.nushungry.reviewservice.service.PriceCalculationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminReviewController 单元测试
 *
 * 测试要点:
 * 1. @WebMvcTest 排除 Security、MongoDB、RabbitMQ 配置
 * 2. Mock Service 层依赖
 * 3. 验证管理员功能: 分页查询、统计、删除、批量删除
 */
@WebMvcTest(
    controllers = {
        AdminReviewController.class,
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
                com.nushungry.reviewservice.filter.JwtAuthenticationFilter.class,
                com.nushungry.reviewservice.config.MongoConfig.class,
                com.nushungry.reviewservice.config.RabbitMQConfig.class,
                com.nushungry.reviewservice.config.SecurityConfig.class
            }
        )
    })
class AdminReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewRepository reviewRepository;

    @MockBean
    private ReviewService reviewService;

    @MockBean
    private RatingCalculationService ratingCalculationService;

    @MockBean
    private PriceCalculationService priceCalculationService;

    @MockBean
    private MongoTemplate mongoTemplate;

    private ReviewDocument testReview;
    private List<ReviewDocument> testReviews;

    @BeforeEach
    void setUp() {
        testReview = new ReviewDocument();
        testReview.setId("review123");
        testReview.setStallId(1L);
        testReview.setStallName("Test Stall");
        testReview.setUserId("user123");
        testReview.setUsername("testuser");
        testReview.setRating(5);
        testReview.setComment("Great food!");
        testReview.setTotalCost(15.0);
        testReview.setNumberOfPeople(2);
        testReview.setCreatedAt(LocalDateTime.now());

        testReviews = Arrays.asList(testReview);
    }

    @Test
    void getAllReviews_NoFilters_Success() throws Exception {
        Page<ReviewDocument> page = new PageImpl<>(testReviews, PageRequest.of(0, 20), 1);
        when(reviewRepository.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/reviews")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviews[0].id").value("review123"))
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20));

        verify(reviewRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void getAllReviews_WithKeywordFilter() throws Exception {
        // Mock MongoTemplate for filtered queries
        when(mongoTemplate.count(any(), eq(ReviewDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(), eq(ReviewDocument.class))).thenReturn(testReviews);

        mockMvc.perform(get("/api/admin/reviews")
                        .param("keyword", "Great")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviews[0].comment").value("Great food!"));

        verify(mongoTemplate, times(1)).count(any(), eq(ReviewDocument.class));
        verify(mongoTemplate, times(1)).find(any(), eq(ReviewDocument.class));
    }

    @Test
    void getAllReviews_WithRatingFilter() throws Exception {
        when(mongoTemplate.count(any(), eq(ReviewDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(), eq(ReviewDocument.class))).thenReturn(testReviews);

        mockMvc.perform(get("/api/admin/reviews")
                        .param("rating", "5")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviews[0].rating").value(5));

        verify(mongoTemplate, times(1)).count(any(), eq(ReviewDocument.class));
    }

    @Test
    void getAllReviews_WithStallIdFilter() throws Exception {
        when(mongoTemplate.count(any(), eq(ReviewDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(), eq(ReviewDocument.class))).thenReturn(testReviews);

        mockMvc.perform(get("/api/admin/reviews")
                        .param("stallId", "1")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviews[0].stallId").value(1));

        verify(mongoTemplate, times(1)).count(any(), eq(ReviewDocument.class));
    }

    @Test
    void getAllReviews_WithUserIdFilter() throws Exception {
        when(mongoTemplate.count(any(), eq(ReviewDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(), eq(ReviewDocument.class))).thenReturn(testReviews);

        mockMvc.perform(get("/api/admin/reviews")
                        .param("userId", "user123")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviews[0].userId").value("user123"));

        verify(mongoTemplate, times(1)).count(any(), eq(ReviewDocument.class));
    }

    @Test
    void getAllReviews_CustomSorting_Asc() throws Exception {
        Page<ReviewDocument> page = new PageImpl<>(testReviews,
            PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "rating")), 1);
        when(reviewRepository.findAll(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/reviews")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "rating")
                        .param("sortDirection", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(reviewRepository, times(1)).findAll(any(Pageable.class));
    }

    @Test
    void getReviewStats_Success() throws Exception {
        LocalDateTime now = LocalDateTime.now();

        Map<Integer, Long> ratingDistribution = new HashMap<>();
        ratingDistribution.put(5, 10L);
        ratingDistribution.put(4, 5L);
        ratingDistribution.put(3, 2L);
        ratingDistribution.put(2, 1L);
        ratingDistribution.put(1, 0L);

        ReviewStatsResponse stats = ReviewStatsResponse.builder()
                .totalReviews(18L)
                .averageRating(4.5)
                .ratingDistribution(ratingDistribution)
                .todayCount(3L)
                .thisWeekCount(10L)
                .thisMonthCount(15L)
                .build();

        // Mock repository calls
        when(reviewRepository.count()).thenReturn(18L);

        List<ReviewDocument> allReviews = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ReviewDocument review = new ReviewDocument();
            review.setRating(5);
            allReviews.add(review);
        }
        for (int i = 0; i < 5; i++) {
            ReviewDocument review = new ReviewDocument();
            review.setRating(4);
            allReviews.add(review);
        }
        for (int i = 0; i < 2; i++) {
            ReviewDocument review = new ReviewDocument();
            review.setRating(3);
            allReviews.add(review);
        }
        ReviewDocument review2Star = new ReviewDocument();
        review2Star.setRating(2);
        allReviews.add(review2Star);

        when(reviewRepository.findAll()).thenReturn(allReviews);
        when(reviewRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(3L, 10L, 15L);

        mockMvc.perform(get("/api/admin/reviews/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalReviews").value(18))
                .andExpect(jsonPath("$.data.ratingDistribution.5").value(10))
                .andExpect(jsonPath("$.data.ratingDistribution.4").value(5))
                .andExpect(jsonPath("$.data.ratingDistribution.3").value(2))
                .andExpect(jsonPath("$.data.ratingDistribution.2").value(1))
                .andExpect(jsonPath("$.data.ratingDistribution.1").value(0))
                .andExpect(jsonPath("$.data.todayCount").value(3))
                .andExpect(jsonPath("$.data.thisWeekCount").value(10))
                .andExpect(jsonPath("$.data.thisMonthCount").value(15));

        verify(reviewRepository, times(1)).count();
        verify(reviewRepository, times(1)).findAll();
        verify(reviewRepository, times(3)).countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class));
    }

    @Test
    void getReviewStats_EmptyDatabase() throws Exception {
        when(reviewRepository.count()).thenReturn(0L);
        when(reviewRepository.findAll()).thenReturn(Collections.emptyList());
        when(reviewRepository.countByCreatedAtBetween(any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(0L);

        mockMvc.perform(get("/api/admin/reviews/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalReviews").value(0))
                .andExpect(jsonPath("$.data.averageRating").value(0.0))
                .andExpect(jsonPath("$.data.ratingDistribution.1").value(0))
                .andExpect(jsonPath("$.data.todayCount").value(0));

        verify(reviewRepository, times(1)).count();
    }

    @Test
    void deleteReview_Success() throws Exception {
        when(reviewRepository.findById(anyString())).thenReturn(Optional.of(testReview));
        doNothing().when(reviewRepository).deleteById(anyString());
        doNothing().when(ratingCalculationService).calculateAndPublishRating(anyLong());
        doNothing().when(priceCalculationService).calculateAndPublishPrice(anyLong());

        mockMvc.perform(delete("/api/admin/reviews/review123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("评价删除成功"));

        verify(reviewRepository, times(1)).findById(eq("review123"));
        verify(reviewRepository, times(1)).deleteById(eq("review123"));
        verify(ratingCalculationService, times(1)).calculateAndPublishRating(eq(1L));
        verify(priceCalculationService, times(1)).calculateAndPublishPrice(eq(1L));
    }

    @Test
    void deleteReview_NotFound() throws Exception {
        when(reviewRepository.findById(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/admin/reviews/nonexistent"))
                .andExpect(status().isInternalServerError());

        verify(reviewRepository, times(1)).findById(eq("nonexistent"));
        verify(reviewRepository, never()).deleteById(anyString());
        verify(ratingCalculationService, never()).calculateAndPublishRating(anyLong());
    }

    @Test
    void batchDeleteReviews_Success() throws Exception {
        ReviewDocument review1 = new ReviewDocument();
        review1.setId("review1");
        review1.setStallId(1L);

        ReviewDocument review2 = new ReviewDocument();
        review2.setId("review2");
        review2.setStallId(2L);

        ReviewDocument review3 = new ReviewDocument();
        review3.setId("review3");
        review3.setStallId(1L);

        when(reviewRepository.findById("review1")).thenReturn(Optional.of(review1));
        when(reviewRepository.findById("review2")).thenReturn(Optional.of(review2));
        when(reviewRepository.findById("review3")).thenReturn(Optional.of(review3));
        doNothing().when(reviewRepository).deleteById(anyString());
        doNothing().when(ratingCalculationService).calculateAndPublishRating(anyLong());
        doNothing().when(priceCalculationService).calculateAndPublishPrice(anyLong());

        List<String> reviewIds = Arrays.asList("review1", "review2", "review3");

        mockMvc.perform(delete("/api/admin/reviews/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value("批量删除评价成功"));

        verify(reviewRepository, times(3)).findById(anyString());
        verify(reviewRepository, times(3)).deleteById(anyString());
        // 两个不同的档口,应该各调用一次
        verify(ratingCalculationService, times(1)).calculateAndPublishRating(eq(1L));
        verify(ratingCalculationService, times(1)).calculateAndPublishRating(eq(2L));
        verify(priceCalculationService, times(1)).calculateAndPublishPrice(eq(1L));
        verify(priceCalculationService, times(1)).calculateAndPublishPrice(eq(2L));
    }

    @Test
    void batchDeleteReviews_PartialSuccess() throws Exception {
        // 第一个评价存在,第二个不存在
        ReviewDocument review1 = new ReviewDocument();
        review1.setId("review1");
        review1.setStallId(1L);

        when(reviewRepository.findById("review1")).thenReturn(Optional.of(review1));
        when(reviewRepository.findById("review2")).thenReturn(Optional.empty());
        doNothing().when(reviewRepository).deleteById(anyString());
        doNothing().when(ratingCalculationService).calculateAndPublishRating(anyLong());
        doNothing().when(priceCalculationService).calculateAndPublishPrice(anyLong());

        List<String> reviewIds = Arrays.asList("review1", "review2");

        mockMvc.perform(delete("/api/admin/reviews/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 只删除了存在的评价
        verify(reviewRepository, times(1)).deleteById(eq("review1"));
        verify(reviewRepository, never()).deleteById(eq("review2"));
        verify(ratingCalculationService, times(1)).calculateAndPublishRating(eq(1L));
    }

    @Test
    void batchDeleteReviews_EmptyList() throws Exception {
        List<String> reviewIds = Collections.emptyList();

        mockMvc.perform(delete("/api/admin/reviews/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reviewIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(reviewRepository, never()).findById(anyString());
        verify(reviewRepository, never()).deleteById(anyString());
        verify(ratingCalculationService, never()).calculateAndPublishRating(anyLong());
    }

    @Test
    void getAllReviews_InvalidRatingFilter() throws Exception {
        // 无效的评分值(超出1-5范围)会被过滤条件忽略,但仍使用 MongoTemplate 查询
        when(mongoTemplate.count(any(), eq(ReviewDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(), eq(ReviewDocument.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/reviews")
                        .param("rating", "6")  // 无效评分(超出1-5范围)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // rating=6 不在有效范围内,Controller 会忽略此过滤条件但仍通过 MongoTemplate 查询
        verify(mongoTemplate, times(1)).count(any(), eq(ReviewDocument.class));
    }

    @Test
    void getAllReviews_InvalidStallIdFormat() throws Exception {
        // 无效的 stallId 格式会触发 NumberFormatException,但被捕获后继续使用 MongoTemplate 查询
        when(mongoTemplate.count(any(), eq(ReviewDocument.class))).thenReturn(0L);
        when(mongoTemplate.find(any(), eq(ReviewDocument.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/reviews")
                        .param("stallId", "invalid")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // stallId 格式错误会被捕获并记录日志,仍通过 MongoTemplate 查询
        verify(mongoTemplate, times(1)).count(any(), eq(ReviewDocument.class));
    }

    @Test
    void getAllReviews_MultipleFilters() throws Exception {
        when(mongoTemplate.count(any(), eq(ReviewDocument.class))).thenReturn(1L);
        when(mongoTemplate.find(any(), eq(ReviewDocument.class))).thenReturn(testReviews);

        mockMvc.perform(get("/api/admin/reviews")
                        .param("keyword", "Great")
                        .param("rating", "5")
                        .param("stallId", "1")
                        .param("userId", "user123")
                        .param("page", "0")
                        .param("size", "20")
                        .param("sortBy", "createdAt")
                        .param("sortDirection", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviews[0].id").value("review123"));

        verify(mongoTemplate, times(1)).count(any(), eq(ReviewDocument.class));
        verify(mongoTemplate, times(1)).find(any(), eq(ReviewDocument.class));
    }
}
