package com.nushungry.cafeteriaservice.dto;

import com.nushungry.cafeteriaservice.model.Image;
import java.time.LocalDateTime;

/**
 * 图片响应DTO
 */
public class ImageResponse {

    private Long id;
    private String imageUrl;
    private String thumbnailUrl;
    private Image.ImageType type;
    private LocalDateTime uploadedAt;
    private String uploadedBy;

    public ImageResponse() {
    }

    public ImageResponse(Long id, String imageUrl, String thumbnailUrl, Image.ImageType type, LocalDateTime uploadedAt, String uploadedBy) {
        this.id = id;
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.type = type;
        this.uploadedAt = uploadedAt;
        this.uploadedBy = uploadedBy;
    }

    public static ImageResponse fromImage(Image image) {
        return new ImageResponse(
            image.getId(),
            image.getImageUrl(),
            image.getThumbnailUrl(),
            image.getType(),
            image.getUploadedAt(),
            image.getUploadedBy()
        );
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

    public Image.ImageType getType() {
        return type;
    }

    public void setType(Image.ImageType type) {
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
}