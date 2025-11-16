package com.nushungry.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Stall {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "cuisine_type")
    private String cuisineType;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "halal_info")
    private String halalInfo;

    private String contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cafeteria_id", nullable = true)
    @JsonBackReference
    private Cafeteria cafeteria;

    @OneToMany(mappedBy = "stall", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("stall-reviews")
    private List<Review> reviews;

    @OneToMany(mappedBy = "stall", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonManagedReference("stall-images")
    private List<Image> images;

    @Column(name = "average_rating")
    private Double averageRating = 0.0; // 平均评分

    @Column(name = "review_count")
    private Integer reviewCount = 0; // 评价数量

    @Column(name = "average_price")
    private Double averagePrice = 0.0; // 人均价格

    @Column(name = "latitude")
    private Double latitude; // 摊位纬度(可选,如果为null则使用cafeteria的坐标)

    @Column(name = "longitude")
    private Double longitude; // 摊位经度(可选,如果为null则使用cafeteria的坐标)

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

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