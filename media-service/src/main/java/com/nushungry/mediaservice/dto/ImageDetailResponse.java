package com.nushungry.mediaservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ImageDetailResponse {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String url;
    private String thumbnailUrl;
    private String contentType;
    private Long size;
    private String uploadedBy;
    private String type;
    private Long relatedId;
    private String relatedType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}