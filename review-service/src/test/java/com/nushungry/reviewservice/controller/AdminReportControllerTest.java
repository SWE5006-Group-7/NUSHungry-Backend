package com.nushungry.reviewservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nushungry.reviewservice.dto.HandleReportRequest;
import com.nushungry.reviewservice.dto.ReportResponse;
import com.nushungry.reviewservice.dto.ReportStatistics;
import com.nushungry.reviewservice.enums.ReportReason;
import com.nushungry.reviewservice.enums.ReportStatus;
import com.nushungry.reviewservice.service.ReviewReportService;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.*;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * AdminReportController 单元测试
 *
 * 测试要点:
 * 1. @WebMvcTest 排除 Security、MongoDB、RabbitMQ 配置
 * 2. Mock ReviewReportService 依赖
 * 3. 验证管理员功能: 获取举报列表、处理举报、统计信息
 */
@WebMvcTest(
    controllers = {
        AdminReportController.class,
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
class AdminReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewReportService reviewReportService;

    private ReportResponse testReport;
    private List<ReportResponse> testReports;

    @BeforeEach
    void setUp() {
        testReport = ReportResponse.builder()
                .id("report123")
                .reviewId("review123")
                .reporterId("reporter123")
                .reporterName("Reporter User")
                .reason(ReportReason.OFFENSIVE)
                .description("This review contains inappropriate content")
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        testReports = Arrays.asList(testReport);
    }

    @Test
    void getPendingReports_Success() throws Exception {
        Page<ReportResponse> page = new PageImpl<>(testReports, PageRequest.of(0, 20), 1);
        when(reviewReportService.getPendingReports(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/reports/pending")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value("report123"))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(20));

        verify(reviewReportService, times(1)).getPendingReports(any(Pageable.class));
    }

    @Test
    void getPendingReports_EmptyResult() throws Exception {
        Page<ReportResponse> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 20), 0);
        when(reviewReportService.getPendingReports(any(Pageable.class))).thenReturn(emptyPage);

        mockMvc.perform(get("/api/admin/reports/pending")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty())
                .andExpect(jsonPath("$.data.totalItems").value(0));

        verify(reviewReportService, times(1)).getPendingReports(any(Pageable.class));
    }

    @Test
    void getPendingReports_ServiceException() throws Exception {
        when(reviewReportService.getPendingReports(any(Pageable.class)))
                .thenThrow(new RuntimeException("Database connection error"));

        mockMvc.perform(get("/api/admin/reports/pending")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("获取待处理举报失败: Database connection error"));

        verify(reviewReportService, times(1)).getPendingReports(any(Pageable.class));
    }

    @Test
    void getAllReports_Success() throws Exception {
        ReportResponse approvedReport = ReportResponse.builder()
                .id("report456")
                .reviewId("review456")
                .reporterId("reporter456")
                .reporterName("Another Reporter")
                .reason(ReportReason.SPAM)
                .description("Spam content")
                .status(ReportStatus.RESOLVED)
                .handledBy("admin123")
                .handledAt(LocalDateTime.now())
                .handleNote("Confirmed as spam")
                .createdAt(LocalDateTime.now().minusDays(1))
                .build();

        List<ReportResponse> allReports = Arrays.asList(testReport, approvedReport);
        Page<ReportResponse> page = new PageImpl<>(allReports, PageRequest.of(0, 20), 2);
        when(reviewReportService.getAllReports(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/reports")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value("report123"))
                .andExpect(jsonPath("$.data.content[1].id").value("report456"))
                .andExpect(jsonPath("$.data.content[1].status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.totalItems").value(2));

        verify(reviewReportService, times(1)).getAllReports(any(Pageable.class));
    }

    @Test
    void getAllReports_CustomPagination() throws Exception {
        Page<ReportResponse> page = new PageImpl<>(testReports, PageRequest.of(1, 10), 15);
        when(reviewReportService.getAllReports(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/reports")
                        .param("page", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentPage").value(1))
                .andExpect(jsonPath("$.data.pageSize").value(10))
                .andExpect(jsonPath("$.data.totalPages").value(2));

        verify(reviewReportService, times(1)).getAllReports(any(Pageable.class));
    }

    @Test
    void handleReport_Resolve_Success() throws Exception {
        HandleReportRequest request = HandleReportRequest.builder()
                .status(ReportStatus.RESOLVED)
                .handleNote("Confirmed violation")
                .build();

        ReportResponse handledReport = ReportResponse.builder()
                .id("report123")
                .reviewId("review123")
                .reporterId("reporter123")
                .reporterName("Reporter User")
                .reason(ReportReason.OFFENSIVE)
                .description("This review contains inappropriate content")
                .status(ReportStatus.RESOLVED)
                .handledBy("admin")
                .handledAt(LocalDateTime.now())
                .handleNote("Confirmed violation")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewReportService.handleReport(anyString(), any(HandleReportRequest.class), anyString()))
                .thenReturn(handledReport);

        mockMvc.perform(put("/api/admin/reports/report123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("report123"))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.handledBy").value("admin"))
                .andExpect(jsonPath("$.data.handleNote").value("Confirmed violation"));

        verify(reviewReportService, times(1)).handleReport(eq("report123"), any(HandleReportRequest.class), eq("admin"));
    }

    @Test
    void handleReport_Reject_Success() throws Exception {
        HandleReportRequest request = HandleReportRequest.builder()
                .status(ReportStatus.REJECTED)
                .handleNote("No violation found")
                .build();

        ReportResponse handledReport = ReportResponse.builder()
                .id("report123")
                .reviewId("review123")
                .reporterId("reporter123")
                .reporterName("Reporter User")
                .reason(ReportReason.OFFENSIVE)
                .description("This review contains inappropriate content")
                .status(ReportStatus.REJECTED)
                .handledBy("admin")
                .handledAt(LocalDateTime.now())
                .handleNote("No violation found")
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewReportService.handleReport(anyString(), any(HandleReportRequest.class), anyString()))
                .thenReturn(handledReport);

        mockMvc.perform(put("/api/admin/reports/report123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.handleNote").value("No violation found"));

        verify(reviewReportService, times(1)).handleReport(eq("report123"), any(HandleReportRequest.class), eq("admin"));
    }

    @Test
    void handleReport_InvalidRequest_MissingStatus() throws Exception {
        HandleReportRequest invalidRequest = HandleReportRequest.builder()
                .handleNote("Note without status")
                .build();

        // 注意: Controller 层没有 @Valid 注解,验证不会在此层触发
        // Service 层会抛出异常,这里应该期望服务层异常导致的 400 响应
        when(reviewReportService.handleReport(anyString(), any(HandleReportRequest.class), anyString()))
                .thenThrow(new RuntimeException("Status is required"));

        mockMvc.perform(put("/api/admin/reports/report123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(reviewReportService, times(1)).handleReport(eq("report123"), any(HandleReportRequest.class), eq("admin"));
    }

    @Test
    void handleReport_NotFound() throws Exception {
        HandleReportRequest request = HandleReportRequest.builder()
                .status(ReportStatus.RESOLVED)
                .handleNote("Confirmed violation")
                .build();

        when(reviewReportService.handleReport(anyString(), any(HandleReportRequest.class), anyString()))
                .thenThrow(new RuntimeException("Report not found"));

        mockMvc.perform(put("/api/admin/reports/nonexistent")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("处理举报失败: Report not found"));

        verify(reviewReportService, times(1)).handleReport(eq("nonexistent"), any(HandleReportRequest.class), eq("admin"));
    }

    @Test
    void getReportStatistics_Success() throws Exception {
        Map<String, Long> statusDistribution = new HashMap<>();
        statusDistribution.put("PENDING", 10L);
        statusDistribution.put("RESOLVED", 5L);
        statusDistribution.put("REJECTED", 3L);

        Map<String, Long> reasonDistribution = new HashMap<>();
        reasonDistribution.put("OFFENSIVE", 8L);
        reasonDistribution.put("SPAM", 5L);
        reasonDistribution.put("FAKE", 3L);
        reasonDistribution.put("OTHER", 2L);

        ReportStatistics stats = ReportStatistics.builder()
                .totalCount(18L)
                .pendingCount(10L)
                .processedCount(5L)
                .rejectedCount(3L)
                .statusDistribution(statusDistribution)
                .reasonDistribution(reasonDistribution)
                .todayCount(5L)
                .thisWeekCount(12L)
                .thisMonthCount(18L)
                .build();

        when(reviewReportService.getStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/admin/reports/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(18))
                .andExpect(jsonPath("$.data.pendingCount").value(10))
                .andExpect(jsonPath("$.data.processedCount").value(5))
                .andExpect(jsonPath("$.data.rejectedCount").value(3))
                .andExpect(jsonPath("$.data.todayCount").value(5))
                .andExpect(jsonPath("$.data.thisWeekCount").value(12))
                .andExpect(jsonPath("$.data.thisMonthCount").value(18));

        verify(reviewReportService, times(1)).getStatistics();
    }

    @Test
    void getReportStatistics_EmptyDatabase() throws Exception {
        ReportStatistics emptyStats = ReportStatistics.builder()
                .totalCount(0L)
                .pendingCount(0L)
                .processedCount(0L)
                .rejectedCount(0L)
                .statusDistribution(new HashMap<>())
                .reasonDistribution(new HashMap<>())
                .todayCount(0L)
                .thisWeekCount(0L)
                .thisMonthCount(0L)
                .build();

        when(reviewReportService.getStatistics()).thenReturn(emptyStats);

        mockMvc.perform(get("/api/admin/reports/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(0))
                .andExpect(jsonPath("$.data.pendingCount").value(0));

        verify(reviewReportService, times(1)).getStatistics();
    }

    @Test
    void getReportStatistics_ServiceException() throws Exception {
        when(reviewReportService.getStatistics())
                .thenThrow(new RuntimeException("Statistics calculation error"));

        mockMvc.perform(get("/api/admin/reports/stats"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("获取举报统计失败: Statistics calculation error"));

        verify(reviewReportService, times(1)).getStatistics();
    }

    @Test
    void getReportsByReview_Success() throws Exception {
        ReportResponse report1 = ReportResponse.builder()
                .id("report1")
                .reviewId("review123")
                .reporterId("reporter1")
                .reporterName("Reporter 1")
                .reason(ReportReason.SPAM)
                .status(ReportStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();

        ReportResponse report2 = ReportResponse.builder()
                .id("report2")
                .reviewId("review123")
                .reporterId("reporter2")
                .reporterName("Reporter 2")
                .reason(ReportReason.OFFENSIVE)
                .status(ReportStatus.RESOLVED)
                .handledBy("admin")
                .handledAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now().minusHours(1))
                .build();

        List<ReportResponse> reports = Arrays.asList(report1, report2);
        when(reviewReportService.getReportsByReviewId(anyString())).thenReturn(reports);

        mockMvc.perform(get("/api/admin/reports/review/review123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reports[0].id").value("report1"))
                .andExpect(jsonPath("$.data.reports[1].id").value("report2"))
                .andExpect(jsonPath("$.data.total").value(2));

        verify(reviewReportService, times(1)).getReportsByReviewId(eq("review123"));
    }

    @Test
    void getReportsByReview_NoReports() throws Exception {
        when(reviewReportService.getReportsByReviewId(anyString())).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/admin/reports/review/review456"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reports").isEmpty())
                .andExpect(jsonPath("$.data.total").value(0));

        verify(reviewReportService, times(1)).getReportsByReviewId(eq("review456"));
    }

    @Test
    void getReportsByReview_ServiceException() throws Exception {
        when(reviewReportService.getReportsByReviewId(anyString()))
                .thenThrow(new RuntimeException("Review not found"));

        mockMvc.perform(get("/api/admin/reports/review/nonexistent"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("获取评价举报记录失败: Review not found"));

        verify(reviewReportService, times(1)).getReportsByReviewId(eq("nonexistent"));
    }

    @Test
    void handleReport_WithoutHandleNote() throws Exception {
        // 处理举报时 handleNote 是可选的
        HandleReportRequest request = HandleReportRequest.builder()
                .status(ReportStatus.RESOLVED)
                .build();

        ReportResponse handledReport = ReportResponse.builder()
                .id("report123")
                .reviewId("review123")
                .reporterId("reporter123")
                .reporterName("Reporter User")
                .reason(ReportReason.OFFENSIVE)
                .status(ReportStatus.RESOLVED)
                .handledBy("admin")
                .handledAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        when(reviewReportService.handleReport(anyString(), any(HandleReportRequest.class), anyString()))
                .thenReturn(handledReport);

        mockMvc.perform(put("/api/admin/reports/report123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        verify(reviewReportService, times(1)).handleReport(eq("report123"), any(HandleReportRequest.class), eq("admin"));
    }

    @Test
    void getPendingReports_DefaultPagination() throws Exception {
        Page<ReportResponse> page = new PageImpl<>(testReports, PageRequest.of(0, 20), 1);
        when(reviewReportService.getPendingReports(any(Pageable.class))).thenReturn(page);

        // 不提供分页参数,应该使用默认值 page=0, size=20
        mockMvc.perform(get("/api/admin/reports/pending"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20));

        verify(reviewReportService, times(1)).getPendingReports(any(Pageable.class));
    }

    @Test
    void getAllReports_DefaultPagination() throws Exception {
        Page<ReportResponse> page = new PageImpl<>(testReports, PageRequest.of(0, 20), 1);
        when(reviewReportService.getAllReports(any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/admin/reports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentPage").value(0))
                .andExpect(jsonPath("$.data.pageSize").value(20));

        verify(reviewReportService, times(1)).getAllReports(any(Pageable.class));
    }
}
