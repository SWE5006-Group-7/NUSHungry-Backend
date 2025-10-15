package com.nushungry.dto;

import com.nushungry.model.ReportReason;
import com.nushungry.model.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 举报响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {
    private Long id;

    // Review信息
    private ReviewInfo review;

    // Reporter信息
    private UserInfo reporter;

    private ReportReason reason;
    private String reasonDisplayName;
    private String description;
    private ReportStatus status;
    private String statusDisplayName;
    private String handledBy;
    private LocalDateTime handledAt;
    private String handleNote;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewInfo {
        private Long id;
        private String comment;
        private Double rating;
        private UserInfo user;
        private StallInfo stall;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String username;
        private String avatarUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StallInfo {
        private Long id;
        private String name;
    }
}
