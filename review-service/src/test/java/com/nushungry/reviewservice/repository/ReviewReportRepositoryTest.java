package com.nushungry.reviewservice.repository;

import com.nushungry.reviewservice.document.ReviewReportDocument;
import com.nushungry.reviewservice.enums.ReportReason;
import com.nushungry.reviewservice.enums.ReportStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
class ReviewReportRepositoryTest {

    @Autowired
    private ReviewReportRepository reviewReportRepository;

    @BeforeEach
    void setUp() {
        reviewReportRepository.deleteAll();
    }

    @Test
    void testSaveReviewReport() {
        ReviewReportDocument report = createReport("review1", "user1", ReportReason.SPAM, ReportStatus.PENDING);
        
        ReviewReportDocument saved = reviewReportRepository.save(report);
        
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getReviewId()).isEqualTo("review1");
        assertThat(saved.getReporterId()).isEqualTo("user1");
    }

    @Test
    void testFindByReviewId() {
        reviewReportRepository.save(createReport("review1", "user1", ReportReason.SPAM, ReportStatus.PENDING));
        reviewReportRepository.save(createReport("review1", "user2", ReportReason.OFFENSIVE, ReportStatus.PENDING));
        reviewReportRepository.save(createReport("review2", "user3", ReportReason.FAKE, ReportStatus.PENDING));

        List<ReviewReportDocument> reports = reviewReportRepository.findByReviewId("review1");

        assertThat(reports).hasSize(2);
        assertThat(reports)
            .extracting(ReviewReportDocument::getReviewId)
            .containsOnly("review1");
    }

    @Test
    void testFindByStatus() {
        reviewReportRepository.save(createReport("review1", "user1", ReportReason.SPAM, ReportStatus.PENDING));
        reviewReportRepository.save(createReport("review2", "user2", ReportReason.OFFENSIVE, ReportStatus.PENDING));
        reviewReportRepository.save(createReport("review3", "user3", ReportReason.FAKE, ReportStatus.RESOLVED));
        reviewReportRepository.save(createReport("review4", "user4", ReportReason.OTHER, ReportStatus.REJECTED));

        Pageable pageable = PageRequest.of(0, 10);
        
        Page<ReviewReportDocument> pendingReports = reviewReportRepository.findByStatus(ReportStatus.PENDING, pageable);
        assertThat(pendingReports.getContent()).hasSize(2);

        Page<ReviewReportDocument> resolvedReports = reviewReportRepository.findByStatus(ReportStatus.RESOLVED, pageable);
        assertThat(resolvedReports.getContent()).hasSize(1);

        Page<ReviewReportDocument> rejectedReports = reviewReportRepository.findByStatus(ReportStatus.REJECTED, pageable);
        assertThat(rejectedReports.getContent()).hasSize(1);
    }

    @Test
    void testExistsByReviewIdAndReporterId() {
        reviewReportRepository.save(createReport("review1", "user1", ReportReason.SPAM, ReportStatus.PENDING));

        boolean exists = reviewReportRepository.existsByReviewIdAndReporterId("review1", "user1");
        assertThat(exists).isTrue();

        boolean notExists = reviewReportRepository.existsByReviewIdAndReporterId("review1", "user2");
        assertThat(notExists).isFalse();
    }

    @Test
    void testMultipleReportsForSameReview() {
        reviewReportRepository.save(createReport("review1", "user1", ReportReason.SPAM, ReportStatus.PENDING));
        reviewReportRepository.save(createReport("review1", "user2", ReportReason.OFFENSIVE, ReportStatus.PENDING));
        reviewReportRepository.save(createReport("review1", "user3", ReportReason.FAKE, ReportStatus.RESOLVED));

        List<ReviewReportDocument> reports = reviewReportRepository.findByReviewId("review1");
        assertThat(reports).hasSize(3);

        List<ReviewReportDocument> pendingReports = reports.stream()
            .filter(r -> r.getStatus() == ReportStatus.PENDING)
            .toList();
        assertThat(pendingReports).hasSize(2);
    }

    @Test
    void testReportStatusTransition() {
        ReviewReportDocument report = createReport("review1", "user1", ReportReason.SPAM, ReportStatus.PENDING);
        ReviewReportDocument saved = reviewReportRepository.save(report);

        saved.setStatus(ReportStatus.RESOLVED);
        saved.setHandledBy("admin1");
        saved.setHandledAt(LocalDateTime.now());
        saved.setHandleNote("Confirmed spam content");
        reviewReportRepository.save(saved);

        ReviewReportDocument updated = reviewReportRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(updated.getHandledBy()).isEqualTo("admin1");
        assertThat(updated.getHandledAt()).isNotNull();
        assertThat(updated.getHandleNote()).isEqualTo("Confirmed spam content");
    }

    @Test
    void testDifferentReportReasons() {
        reviewReportRepository.save(createReport("review1", "user1", ReportReason.SPAM, ReportStatus.PENDING));
        reviewReportRepository.save(createReport("review2", "user2", ReportReason.OFFENSIVE, ReportStatus.PENDING));
        reviewReportRepository.save(createReport("review3", "user3", ReportReason.FAKE, ReportStatus.PENDING));
        reviewReportRepository.save(createReport("review4", "user4", ReportReason.OTHER, ReportStatus.PENDING));

        List<ReviewReportDocument> allReports = reviewReportRepository.findAll();
        assertThat(allReports).hasSize(4);
        assertThat(allReports)
            .extracting(ReviewReportDocument::getReason)
            .containsExactlyInAnyOrder(
                ReportReason.SPAM,
                ReportReason.OFFENSIVE,
                ReportReason.FAKE,
                ReportReason.OTHER
            );
    }

    @Test
    void testDeleteReport() {
        ReviewReportDocument report = createReport("review1", "user1", ReportReason.SPAM, ReportStatus.PENDING);
        ReviewReportDocument saved = reviewReportRepository.save(report);

        reviewReportRepository.deleteById(saved.getId());

        assertThat(reviewReportRepository.findById(saved.getId())).isEmpty();
    }

    private ReviewReportDocument createReport(String reviewId, String reporterId, 
                                              ReportReason reason, ReportStatus status) {
        ReviewReportDocument report = new ReviewReportDocument();
        report.setReviewId(reviewId);
        report.setReporterId(reporterId);
        report.setReporterName("Reporter " + reporterId);
        report.setReason(reason);
        report.setDescription("Test description for " + reason);
        report.setStatus(status);
        report.setCreatedAt(LocalDateTime.now());
        return report;
    }
}
