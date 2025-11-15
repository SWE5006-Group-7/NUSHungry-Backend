package com.nushungry.reviewservice.service;

import com.nushungry.reviewservice.document.ReviewReportDocument;
import com.nushungry.reviewservice.dto.CreateReportRequest;
import com.nushungry.reviewservice.dto.HandleReportRequest;
import com.nushungry.reviewservice.dto.ReportResponse;
import com.nushungry.reviewservice.enums.ReportReason;
import com.nushungry.reviewservice.enums.ReportStatus;
import com.nushungry.reviewservice.exception.ResourceNotFoundException;
import com.nushungry.reviewservice.exception.ValidationException;
import com.nushungry.reviewservice.repository.ReviewReportRepository;
import com.nushungry.reviewservice.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewReportServiceTest {

    @Mock
    private ReviewReportRepository reviewReportRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewReportService reviewReportService;

    private ReviewReportDocument testReport;
    private CreateReportRequest createRequest;
    private HandleReportRequest handleRequest;

    @BeforeEach
    void setUp() {
        testReport = new ReviewReportDocument();
        testReport.setId("report1");
        testReport.setReviewId("review1");
        testReport.setReporterId("user1");
        testReport.setReporterName("Reporter User");
        testReport.setReason(ReportReason.SPAM);
        testReport.setDescription("This is spam");
        testReport.setStatus(ReportStatus.PENDING);
        testReport.setCreatedAt(LocalDateTime.now());

        createRequest = CreateReportRequest.builder()
                .reason(ReportReason.SPAM)
                .description("This is spam")
                .build();

        handleRequest = HandleReportRequest.builder()
                .status(ReportStatus.RESOLVED)
                .handleNote("Confirmed spam")
                .build();
    }

    @Test
    void testCreateReport() {
        when(reviewRepository.existsById("review1")).thenReturn(true);
        when(reviewReportRepository.existsByReviewIdAndReporterId("review1", "user1")).thenReturn(false);
        when(reviewReportRepository.save(any(ReviewReportDocument.class))).thenReturn(testReport);

        ReportResponse response = reviewReportService.createReport(
                "review1", createRequest, "user1", "Reporter User");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("report1");
        assertThat(response.getReason()).isEqualTo(ReportReason.SPAM);
        assertThat(response.getStatus()).isEqualTo(ReportStatus.PENDING);

        verify(reviewRepository).existsById("review1");
        verify(reviewReportRepository).save(any(ReviewReportDocument.class));
    }

    @Test
    void testCreateReportAlreadyReported() {
        when(reviewRepository.existsById("review1")).thenReturn(true);
        when(reviewReportRepository.existsByReviewIdAndReporterId("review1", "user1")).thenReturn(true);

        assertThatThrownBy(() -> reviewReportService.createReport(
                "review1", createRequest, "user1", "Reporter User"))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("already reported");

        verify(reviewRepository).existsById("review1");
        verify(reviewReportRepository, never()).save(any());
    }

    @Test
    void testGetReportsByReviewId() {
        List<ReviewReportDocument> reports = Arrays.asList(testReport);
        when(reviewReportRepository.findByReviewId("review1")).thenReturn(reports);

        List<ReportResponse> result = reviewReportService.getReportsByReviewId("review1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo("report1");

        verify(reviewReportRepository).findByReviewId("review1");
    }

    @Test
    void testGetReportsByStatus() {
        List<ReviewReportDocument> reports = Arrays.asList(testReport);
        Page<ReviewReportDocument> page = new PageImpl<>(reports);
        Pageable pageable = PageRequest.of(0, 10);

        when(reviewReportRepository.findByStatus(ReportStatus.PENDING, pageable)).thenReturn(page);

        Page<ReportResponse> result = reviewReportService.getReportsByStatus(ReportStatus.PENDING, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ReportStatus.PENDING);

        verify(reviewReportRepository).findByStatus(ReportStatus.PENDING, pageable);
    }

    @Test
    void testHandleReport() {
        when(reviewReportRepository.findById("report1")).thenReturn(Optional.of(testReport));
        when(reviewReportRepository.save(any(ReviewReportDocument.class))).thenReturn(testReport);

        ReportResponse response = reviewReportService.handleReport(
                "report1", handleRequest, "admin1");

        assertThat(response).isNotNull();
        assertThat(testReport.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(testReport.getHandledBy()).isEqualTo("admin1");
        assertThat(testReport.getHandleNote()).isEqualTo("Confirmed spam");
        assertThat(testReport.getHandledAt()).isNotNull();

        verify(reviewReportRepository).save(testReport);
    }

    @Test
    void testHandleReportNotFound() {
        when(reviewReportRepository.findById("report1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewReportService.handleReport(
                "report1", handleRequest, "admin1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Report");

        verify(reviewReportRepository, never()).save(any());
    }

    @Test
    void testHandleReportWithReject() {
        handleRequest.setStatus(ReportStatus.REJECTED);
        handleRequest.setHandleNote("False report");

        when(reviewReportRepository.findById("report1")).thenReturn(Optional.of(testReport));
        when(reviewReportRepository.save(any(ReviewReportDocument.class))).thenReturn(testReport);

        ReportResponse response = reviewReportService.handleReport(
                "report1", handleRequest, "admin1");

        assertThat(testReport.getStatus()).isEqualTo(ReportStatus.REJECTED);
        verify(reviewReportRepository).save(testReport);
    }

    @Test
    void testCreateReportWithDifferentReasons() {
        when(reviewRepository.existsById(anyString())).thenReturn(true);
        when(reviewReportRepository.existsByReviewIdAndReporterId(anyString(), anyString())).thenReturn(false);
        when(reviewReportRepository.save(any(ReviewReportDocument.class))).thenReturn(testReport);

        createRequest.setReason(ReportReason.OFFENSIVE);
        reviewReportService.createReport("review1", createRequest, "user1", "Reporter");

        createRequest.setReason(ReportReason.FAKE);
        reviewReportService.createReport("review2", createRequest, "user1", "Reporter");

        createRequest.setReason(ReportReason.OTHER);
        reviewReportService.createReport("review3", createRequest, "user1", "Reporter");

        verify(reviewRepository, times(3)).existsById(anyString());
        verify(reviewReportRepository, times(3)).save(any(ReviewReportDocument.class));
    }
}
