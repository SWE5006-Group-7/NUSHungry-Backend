package com.nushungry.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String author; // Username for backward compatibility

    @Column(nullable = false)
    private Double rating; // 1.0 to 5.0 (allows half stars)

    @Column(columnDefinition = "TEXT")
    private String comment; // 评价内容

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "review_images", joinColumns = @JoinColumn(name = "review_id"))
    @Column(name = "image_url", length = 500)
    private List<String> imageUrls = new ArrayList<>(); // 评价图片列表

    @Column(columnDefinition = "boolean default false")
    private boolean processed = false; // For tracking if a complaint/low rating has been handled

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", nullable = false)
    private ModerationStatus moderationStatus = ModerationStatus.PENDING; // 审核状态

    @Column(name = "reject_reason")
    private String rejectReason; // 驳回原因

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt; // 审核时间

    @Column(name = "moderated_by")
    private String moderatedBy; // 审核人(管理员用户名)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stall_id", nullable = false)
    @JsonBackReference
    private Stall stall;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}