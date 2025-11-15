package com.nushungry.reviewservice.document;

import com.nushungry.reviewservice.enums.ReportReason;
import com.nushungry.reviewservice.enums.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "review_reports")
public class ReviewReportDocument {

    @Id
    private String id;

    @Indexed
    private String reviewId;

    @Indexed
    private String reporterId;

    private String reporterName;

    private ReportReason reason;

    private String description;

    @Indexed
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    private String handledBy;

    private LocalDateTime handledAt;

    private String handleNote;

    @CreatedDate
    private LocalDateTime createdAt;
}
