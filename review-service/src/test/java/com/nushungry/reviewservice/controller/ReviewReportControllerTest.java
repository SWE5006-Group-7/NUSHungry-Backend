package com.nushungry.reviewservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.reviewservice.dto.CreateReportRequest;
import com.nushungry.reviewservice.dto.HandleReportRequest;
import com.nushungry.reviewservice.dto.ReportResponse;
import com.nushungry.reviewservice.enums.ReportReason;
import com.nushungry.reviewservice.enums.ReportStatus;
import com.nushungry.reviewservice.exception.ResourceNotFoundException;
import com.nushungry.reviewservice.exception.ValidationException;
import com.nushungry.reviewservice.service.ReviewReportService;
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
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
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
        ReviewReportController.class,
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
class ReviewReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewReportService reviewReportService;

    private ReportResponse reportResponse;
    private CreateReportRequest createReportRequest;
    private HandleReportRequest handleReportRequest;

    @BeforeEach
    void setUp() {
        reportResponse = ReportResponse.builder()
                .id("report123")
                .reviewId("review123")
                .reporterId("user123")
                .reporterName("testuser")
                .reason(ReportReason.SPAM)
                .description("This is spam")
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        createReportRequest = CreateReportRequest.builder()
                .reason(ReportReason.SPAM)
                .description("This is spam")
                .build();

        handleReportRequest = HandleReportRequest.builder()
                .status(ReportStatus.RESOLVED)
                .handleNote("Removed the review")
                .build();
    }

    @Test
    void createReport_Success() throws Exception {
        when(reviewReportService.createReport(anyString(), any(CreateReportRequest.class), anyString(), anyString()))
                .thenReturn(reportResponse);

        mockMvc.perform(post("/api/reviews/review123/report")
                        .header("X-User-Id", "user123")
                        .header("X-Username", "testuser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReportRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Report submitted successfully"))
                .andExpect(jsonPath("$.data.id").value("report123"))
                .andExpect(jsonPath("$.data.reviewId").value("review123"))
                .andExpect(jsonPath("$.data.reason").value("SPAM"));

        verify(reviewReportService, times(1)).createReport(eq("review123"), any(CreateReportRequest.class), eq("user123"), eq("testuser"));
    }

    @Test
    void createReport_ReviewNotFound() throws Exception {
        when(reviewReportService.createReport(anyString(), any(CreateReportRequest.class), anyString(), anyString()))
                .thenThrow(new ResourceNotFoundException("Review not found"));

        mockMvc.perform(post("/api/reviews/nonexistent/report")
                        .header("X-User-Id", "user123")
                        .header("X-Username", "testuser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReportRequest)))
                .andExpect(status().isNotFound());

        verify(reviewReportService, times(1)).createReport(eq("nonexistent"), any(CreateReportRequest.class), eq("user123"), eq("testuser"));
    }

    @Test
    void createReport_DuplicateReport() throws Exception {
        when(reviewReportService.createReport(anyString(), any(CreateReportRequest.class), anyString(), anyString()))
                .thenThrow(new ValidationException("You have already reported this review"));

        mockMvc.perform(post("/api/reviews/review123/report")
                        .header("X-User-Id", "user123")
                        .header("X-Username", "testuser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReportRequest)))
                .andExpect(status().isBadRequest());

        verify(reviewReportService, times(1)).createReport(eq("review123"), any(CreateReportRequest.class), eq("user123"), eq("testuser"));
    }

    @Test
    void createReport_MissingHeaders() throws Exception {
        mockMvc.perform(post("/api/reviews/review123/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReportRequest)))
                .andExpect(status().isBadRequest());

        verify(reviewReportService, never()).createReport(anyString(), any(), anyString(), anyString());
    }

    @Test
    void getReportsByReviewId_Admin_Success() throws Exception {
        List<ReportResponse> reports = Arrays.asList(reportResponse);
        when(reviewReportService.getReportsByReviewId(anyString())).thenReturn(reports);

        mockMvc.perform(get("/api/reviews/review123/reports")
                        .header("X-User-Role", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("report123"))
                .andExpect(jsonPath("$.data[0].reviewId").value("review123"));

        verify(reviewReportService, times(1)).getReportsByReviewId(eq("review123"));
    }

    @Test
    void getReportsByReviewId_NonAdmin_Forbidden() throws Exception {
        mockMvc.perform(get("/api/reviews/review123/reports")
                        .header("X-User-Role", "ROLE_USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));

        verify(reviewReportService, never()).getReportsByReviewId(anyString());
    }

    @Test
    void getReportsByReviewId_MissingRole_Forbidden() throws Exception {
        mockMvc.perform(get("/api/reviews/review123/reports"))
                .andExpect(status().isBadRequest());

        verify(reviewReportService, never()).getReportsByReviewId(anyString());
    }

    @Test
    void getReportsByStatus_Admin_Success() throws Exception {
        List<ReportResponse> reports = Arrays.asList(reportResponse);
        Page<ReportResponse> page = new PageImpl<>(reports, PageRequest.of(0, 10), 1);

        when(reviewReportService.getReportsByStatus(any(ReportStatus.class), any(Pageable.class)))
                .thenReturn(page);

        mockMvc.perform(get("/api/reviews/reports/status/PENDING")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value("report123"))
                .andExpect(jsonPath("$.data.totalElements").value(1));

        verify(reviewReportService, times(1)).getReportsByStatus(eq(ReportStatus.PENDING), any(Pageable.class));
    }

    @Test
    void getReportsByStatus_NonAdmin_Forbidden() throws Exception {
        mockMvc.perform(get("/api/reviews/reports/status/PENDING")
                        .header("X-User-Role", "ROLE_USER"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));

        verify(reviewReportService, never()).getReportsByStatus(any(), any());
    }

    @Test
    void handleReport_Admin_Success() throws Exception {
        ReportResponse handledReport = ReportResponse.builder()
                .id("report123")
                .reviewId("review123")
                .reporterId("user123")
                .reporterName("testuser")
                .reason(ReportReason.SPAM)
                .description("This is spam")
                .status(ReportStatus.RESOLVED)
                .handledBy("admin123")
                .handledAt(LocalDateTime.now())
                .handleNote("Removed the review")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewReportService.handleReport(anyString(), any(HandleReportRequest.class), anyString()))
                .thenReturn(handledReport);

        mockMvc.perform(put("/api/reviews/reports/report123/handle")
                        .header("X-User-Id", "admin123")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(handleReportRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Report handled successfully"))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.handledBy").value("admin123"));

        verify(reviewReportService, times(1)).handleReport(eq("report123"), any(HandleReportRequest.class), eq("admin123"));
    }

    @Test
    void handleReport_NonAdmin_Forbidden() throws Exception {
        mockMvc.perform(put("/api/reviews/reports/report123/handle")
                        .header("X-User-Id", "user123")
                        .header("X-User-Role", "ROLE_USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(handleReportRequest)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));

        verify(reviewReportService, never()).handleReport(anyString(), any(), anyString());
    }

    @Test
    void handleReport_ReportNotFound() throws Exception {
        when(reviewReportService.handleReport(anyString(), any(HandleReportRequest.class), anyString()))
                .thenThrow(new ResourceNotFoundException("Report not found"));

        mockMvc.perform(put("/api/reviews/reports/nonexistent/handle")
                        .header("X-User-Id", "admin123")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(handleReportRequest)))
                .andExpect(status().isNotFound());

        verify(reviewReportService, times(1)).handleReport(eq("nonexistent"), any(HandleReportRequest.class), eq("admin123"));
    }

    @Test
    void handleReport_InvalidStatus() throws Exception {
        HandleReportRequest invalidRequest = HandleReportRequest.builder()
                .status(null)
                .handleNote("No status")
                .build();

        mockMvc.perform(put("/api/reviews/reports/report123/handle")
                        .header("X-User-Id", "admin123")
                        .header("X-User-Role", "ROLE_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(reviewReportService, never()).handleReport(anyString(), any(), anyString());
    }

    @Test
    void createReport_InvalidReason() throws Exception {
        CreateReportRequest invalidRequest = CreateReportRequest.builder()
                .reason(null)
                .description("No reason")
                .build();

        mockMvc.perform(post("/api/reviews/review123/report")
                        .header("X-User-Id", "user123")
                        .header("X-Username", "testuser")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());

        verify(reviewReportService, never()).createReport(anyString(), any(), anyString(), anyString());
    }

    @Test
    void getReportsByStatus_AllStatuses() throws Exception {
        for (ReportStatus status : ReportStatus.values()) {
            Page<ReportResponse> page = new PageImpl<>(Arrays.asList(reportResponse), PageRequest.of(0, 10), 1);
            when(reviewReportService.getReportsByStatus(eq(status), any(Pageable.class)))
                    .thenReturn(page);

            mockMvc.perform(get("/api/reviews/reports/status/" + status.name())
                            .header("X-User-Role", "ROLE_ADMIN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        verify(reviewReportService, times(ReportStatus.values().length)).getReportsByStatus(any(ReportStatus.class), any(Pageable.class));
    }
}
