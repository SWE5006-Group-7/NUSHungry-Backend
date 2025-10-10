package com.nushungry.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 审核日志实体
 * 记录所有审核操作的历史
 */
@Data
@Entity
@Table(name = "moderation_log")
public class ModerationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id", nullable = false)
    private User moderator; // 审核人

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false)
    private ModerationStatus action; // 审核动作 (APPROVED or REJECTED)

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason; // 审核原因/备注

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
