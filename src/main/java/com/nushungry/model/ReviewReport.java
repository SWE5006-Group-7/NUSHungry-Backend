package com.nushungry.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评价举报实体
 * 用户可以举报不当评价内容
 */
@Data
@Entity
@Table(name = "review_reports")
public class ReviewReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter; // 举报人

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false)
    private ReportReason reason; // 举报原因

    @Column(name = "description", columnDefinition = "TEXT")
    private String description; // 详细描述

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status = ReportStatus.PENDING; // 处理状态

    @Column(name = "handled_by")
    private String handledBy; // 处理人(管理员用户名)

    @Column(name = "handled_at")
    private LocalDateTime handledAt; // 处理时间

    @Column(name = "handle_note", length = 500)
    private String handleNote; // 处理备注

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
