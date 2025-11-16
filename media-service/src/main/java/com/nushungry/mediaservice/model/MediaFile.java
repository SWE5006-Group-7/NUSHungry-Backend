package com.nushungry.mediaservice.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Data
public class MediaFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private String originalFileName;
    private String url;
    private String thumbnailUrl;
    private String contentType;
    private Long size;
    private String uploadedBy;
    private String type; // PHOTO, MENU, AVATAR, etc.
    private Long relatedId; // 关联的食堂或摊位ID
    private String relatedType; // cafeteria, stall, user, etc.

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum MediaType {
        PHOTO, MENU, AVATAR, OTHER
    }
}