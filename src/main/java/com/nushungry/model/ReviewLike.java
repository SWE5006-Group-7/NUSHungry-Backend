package com.nushungry.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 评价点赞实体
 * 记录用户对评价的点赞行为
 */
@Data
@Entity
@Table(
    name = "review_likes",
    indexes = {
        @Index(name = "idx_review_id", columnList = "review_id"),
        @Index(name = "idx_user_id", columnList = "user_id"),
        @Index(name = "idx_review_user", columnList = "review_id,user_id", unique = true)
    }
)
public class ReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
