package com.nushungry.cafeteriaservice.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "images")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String imageUrl;

    @Column(name = "thumbnail_url")
    private String thumbnailUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false)
    private ImageType type = ImageType.PHOTO;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @Column(name = "uploaded_by")
    private String uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cafeteria_id")
    @JsonBackReference("cafeteria-images")
    private Cafeteria cafeteria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stall_id")
    @JsonBackReference("stall-images")
    private Stall stall;

    @PrePersist
    protected void onCreate() {
        uploadedAt = LocalDateTime.now();
    }

    public enum ImageType {
        PHOTO,
        MENU
    }

    public Image() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }

    public ImageType getType() {
        return type;
    }

    public void setType(ImageType type) {
        this.type = type;
    }

    public LocalDateTime getUploadedAt() {
        return uploadedAt;
    }

    public void setUploadedAt(LocalDateTime uploadedAt) {
        this.uploadedAt = uploadedAt;
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public Cafeteria getCafeteria() {
        return cafeteria;
    }

    public void setCafeteria(Cafeteria cafeteria) {
        this.cafeteria = cafeteria;
    }

    public Stall getStall() {
        return stall;
    }

    public void setStall(Stall stall) {
        this.stall = stall;
    }
}


