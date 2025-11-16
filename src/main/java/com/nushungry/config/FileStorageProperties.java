package com.nushungry.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 文件存储配置属性
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "file.storage")
public class FileStorageProperties {

    /**
     * 文件上传根目录
     */
    private String uploadDir = "uploads";

    /**
     * 图片存储目录
     */
    private String imageDir = "images";

    /**
     * 缩略图存储目录
     */
    private String thumbnailDir = "thumbnails";

    /**
     * 最大文件大小（字节）默认10MB
     */
    private long maxFileSize = 10 * 1024 * 1024;

    /**
     * 允许的图片格式
     */
    private String[] allowedImageTypes = {"image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"};

    /**
     * 缩略图宽度
     */
    private int thumbnailWidth = 300;

    /**
     * 缩略图高度
     */
    private int thumbnailHeight = 300;

    /**
     * 图片压缩质量（0.0-1.0）
     */
    private float compressionQuality = 0.85f;

    /**
     * 是否生成缩略图
     */
    private boolean generateThumbnail = true;

    /**
     * 获取完整的图片上传目录路径
     */
    public String getImageUploadPath() {
        return uploadDir + "/" + imageDir;
    }

    /**
     * 获取完整的缩略图上传目录路径
     */
    public String getThumbnailUploadPath() {
        return uploadDir + "/" + thumbnailDir;
    }
}
