package com.nushungry.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评价举报实体
 * 记录用户对评价的举报信息
 */
@Data
@Entity
@Table(
    name = "review_reports",
    indexes = {
        @Index(name = "idx_review_id", columnList = "review_id"),
        @Index(name = "idx_reporter_id", columnList = "reporter_id"),
        @Index(name = "idx_status", columnList = "status")
    }
)
public class ReviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review; // 被举报的评价

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter; // 举报人

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReportReason reason; // 举报原因

    @Column(columnDefinition = "TEXT")
    private String description; // 详细描述

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status = ReportStatus.PENDING; // 处理状态

    @Column(name = "handler_id")
    private Long handlerId; // 处理人ID（管理员）

    @Column(columnDefinition = "TEXT")
    private String handlerNote; // 处理说明

    @Column(name = "handled_at")
    private LocalDateTime handledAt; // 处理时间

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /**
     * 举报原因枚举
     */
    public enum ReportReason {
        SPAM("垃圾信息"),
        INAPPROPRIATE("不当内容"),
        OFFENSIVE("侮辱谩骂"),
        FALSE_INFO("虚假信息"),
        ADVERTISING("广告推广"),
        PRIVACY("侵犯隐私"),
        OTHER("其他");

        private final String description;

        ReportReason(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 举报处理状态枚举
     */
    public enum ReportStatus {
        PENDING("待处理"),
        ACCEPTED("已接受"),
        REJECTED("已驳回");

        private final String description;

        ReportStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
