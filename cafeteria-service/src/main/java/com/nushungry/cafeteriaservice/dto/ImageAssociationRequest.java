package com.nushungry.cafeteriaservice.dto;

import com.nushungry.cafeteriaservice.model.Image;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 图片关联请求DTO
 */
public class ImageAssociationRequest {

    @NotBlank(message = "图片URL不能为空")
    private String imageUrl;

    private String thumbnailUrl;

    @NotNull(message = "图片类型不能为空")
    private Image.ImageType type;

    private String uploadedBy;

    public ImageAssociationRequest() {
    }

    public ImageAssociationRequest(String imageUrl, String thumbnailUrl, Image.ImageType type, String uploadedBy) {
        this.imageUrl = imageUrl;
        this.thumbnailUrl = thumbnailUrl;
        this.type = type;
        this.uploadedBy = uploadedBy;
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

    public String getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(String uploadedBy) {
        this.uploadedBy = uploadedBy;
    }
}