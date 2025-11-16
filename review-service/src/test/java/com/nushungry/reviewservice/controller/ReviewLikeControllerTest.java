package com.nushungry.reviewservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.reviewservice.exception.ResourceNotFoundException;
import com.nushungry.reviewservice.service.ReviewLikeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import com.nushungry.reviewservice.util.JwtUtil;
import com.nushungry.reviewservice.filter.JwtAuthenticationFilter;

@WebMvcTest(controllers = {
        ReviewLikeController.class,
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
        @org.springframework.context.annotation.ComponentScan.Filter(
            type = org.springframework.context.annotation.FilterType.ASSIGNABLE_TYPE,
            classes = {
                JwtAuthenticationFilter.class,
                com.nushungry.reviewservice.config.MongoConfig.class,
                com.nushungry.reviewservice.config.RabbitMQConfig.class,
                com.nushungry.reviewservice.config.SecurityConfig.class
            }
        )
    })
class ReviewLikeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewLikeService reviewLikeService;

    @Test
    void toggleLike_LikeReview() throws Exception {
        when(reviewLikeService.toggleLike(anyString(), anyString())).thenReturn(true);
        when(reviewLikeService.getLikeCount(anyString())).thenReturn(1L);

        mockMvc.perform(post("/api/reviews/review123/like")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Review liked successfully"))
                .andExpect(jsonPath("$.data.isLiked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        verify(reviewLikeService, times(1)).toggleLike(eq("review123"), eq("user123"));
        verify(reviewLikeService, times(1)).getLikeCount(eq("review123"));
    }

    @Test
    void toggleLike_UnlikeReview() throws Exception {
        when(reviewLikeService.toggleLike(anyString(), anyString())).thenReturn(false);
        when(reviewLikeService.getLikeCount(anyString())).thenReturn(0L);

        mockMvc.perform(post("/api/reviews/review123/like")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Review unliked successfully"))
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));

        verify(reviewLikeService, times(1)).toggleLike(eq("review123"), eq("user123"));
        verify(reviewLikeService, times(1)).getLikeCount(eq("review123"));
    }

    @Test
    void toggleLike_ReviewNotFound() throws Exception {
        when(reviewLikeService.toggleLike(anyString(), anyString()))
                .thenThrow(new ResourceNotFoundException("Review not found"));

        mockMvc.perform(post("/api/reviews/nonexistent/like")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isNotFound());

        verify(reviewLikeService, times(1)).toggleLike(eq("nonexistent"), eq("user123"));
    }

    @Test
    void toggleLike_MissingUserId() throws Exception {
        mockMvc.perform(post("/api/reviews/review123/like"))
                .andExpect(status().isBadRequest());

        verify(reviewLikeService, never()).toggleLike(anyString(), anyString());
    }

    @Test
    void isLikedByUser_True() throws Exception {
        when(reviewLikeService.isLikedByUser(anyString(), anyString())).thenReturn(true);

        mockMvc.perform(get("/api/reviews/review123/is-liked")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(true));

        verify(reviewLikeService, times(1)).isLikedByUser(eq("review123"), eq("user123"));
    }

    @Test
    void isLikedByUser_False() throws Exception {
        when(reviewLikeService.isLikedByUser(anyString(), anyString())).thenReturn(false);

        mockMvc.perform(get("/api/reviews/review123/is-liked")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(false));

        verify(reviewLikeService, times(1)).isLikedByUser(eq("review123"), eq("user123"));
    }

    @Test
    void isLikedByUser_ReviewNotFound() throws Exception {
        when(reviewLikeService.isLikedByUser(anyString(), anyString()))
                .thenThrow(new ResourceNotFoundException("Review not found"));

        mockMvc.perform(get("/api/reviews/nonexistent/is-liked")
                        .header("X-User-Id", "user123"))
                .andExpect(status().isNotFound());

        verify(reviewLikeService, times(1)).isLikedByUser(eq("nonexistent"), eq("user123"));
    }

    @Test
    void getLikeCount_Success() throws Exception {
        when(reviewLikeService.getLikeCount(anyString())).thenReturn(10L);

        mockMvc.perform(get("/api/reviews/review123/like-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(10));

        verify(reviewLikeService, times(1)).getLikeCount(eq("review123"));
    }

    @Test
    void getLikeCount_Zero() throws Exception {
        when(reviewLikeService.getLikeCount(anyString())).thenReturn(0L);

        mockMvc.perform(get("/api/reviews/review123/like-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(0));

        verify(reviewLikeService, times(1)).getLikeCount(eq("review123"));
    }

    @Test
    void getLikeCount_ReviewNotFound() throws Exception {
        when(reviewLikeService.getLikeCount(anyString()))
                .thenThrow(new ResourceNotFoundException("Review not found"));

        mockMvc.perform(get("/api/reviews/nonexistent/like-count"))
                .andExpect(status().isNotFound());

        verify(reviewLikeService, times(1)).getLikeCount(eq("nonexistent"));
    }

    @Test
    void toggleLike_MultipleUsers() throws Exception {
        when(reviewLikeService.toggleLike(eq("review123"), eq("user1"))).thenReturn(true);
        when(reviewLikeService.getLikeCount(eq("review123"))).thenReturn(1L, 2L);
        when(reviewLikeService.toggleLike(eq("review123"), eq("user2"))).thenReturn(true);

        mockMvc.perform(post("/api/reviews/review123/like")
                        .header("X-User-Id", "user1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(1));

        mockMvc.perform(post("/api/reviews/review123/like")
                        .header("X-User-Id", "user2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.likeCount").value(2));
    }
}
