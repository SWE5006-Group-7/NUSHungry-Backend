package com.nushungry.mediaservice.service;

import com.nushungry.mediaservice.model.MediaFile;
import com.nushungry.mediaservice.repository.MediaFileRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ImageProcessingService {
    @Value("${media.storage.path}")
    private String storagePath;

    private final MediaFileRepository repository;

    public ImageProcessingService(MediaFileRepository repository) {
        this.repository = repository;
    }

    public MediaFile storeFile(MultipartFile file) throws IOException {
        String fileName = System.currentTimeMillis() + "_" + file.getOriginalFilename();

        // 判断 storagePath 是绝对路径还是相对路径
        File storageDir = new File(storagePath);
        if (!storageDir.isAbsolute()) {
            // 相对路径：拼接到工作目录
            String basePath = System.getProperty("user.dir");
            storageDir = new File(basePath, storagePath);
        }

        File dest = new File(storageDir, fileName);
        dest.getParentFile().mkdirs();
        file.transferTo(dest);

        MediaFile mediaFile = new MediaFile();
        mediaFile.setFileName(fileName);
        mediaFile.setUrl("/media/" + fileName);
        mediaFile.setContentType(file.getContentType());
        mediaFile.setSize(file.getSize());
        return repository.save(mediaFile);
    }

    /**
     * 验证是否为有效图片
     */
    public boolean isValidImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }

        // 检查是否为常见图片格式
        return contentType.startsWith("image/") &&
               (contentType.equals("image/jpeg") ||
                contentType.equals("image/jpg") ||
                contentType.equals("image/png") ||
                contentType.equals("image/gif") ||
                contentType.equals("image/webp"));
    }

    /**
     * 根据URL删除图片
     */
    @Transactional
    public boolean deleteImageByUrl(String imageUrl) {
        try {
            log.info("删除图片: {}", imageUrl);

            // 从数据库中查找对应的记录
            MediaFile mediaFile = repository.findByUrl(imageUrl).orElse(null);
            if (mediaFile == null) {
                log.warn("未找到图片记录: {}", imageUrl);
                return false;
            }

            // 删除物理文件
            boolean fileDeleted = deletePhysicalFile(mediaFile.getFileName());

            if (fileDeleted) {
                // 删除数据库记录
                repository.delete(mediaFile);
                log.info("图片删除成功: {}", imageUrl);
                return true;
            } else {
                log.warn("物理文件删除失败，但删除数据库记录: {}", imageUrl);
                // 即使物理文件删除失败，也删除数据库记录
                repository.delete(mediaFile);
                return true;
            }

        } catch (Exception e) {
            log.error("删除图片失败: {}", imageUrl, e);
            return false;
        }
    }

    /**
     * 批量删除图片
     */
    @Transactional
    public List<String> deleteImagesByUrls(List<String> imageUrls) {
        List<String> deletedUrls = new ArrayList<>();

        for (String imageUrl : imageUrls) {
            if (deleteImageByUrl(imageUrl)) {
                deletedUrls.add(imageUrl);
            }
        }

        log.info("批量删除完成，成功删除: {} 张图片", deletedUrls.size());
        return deletedUrls;
    }

    /**
     * 批量上传图片
     */
    @Transactional
    public BatchUploadResult batchUploadImages(List<MultipartFile> files,
                                              boolean generateThumbnail,
                                              boolean compress) {
        List<ImageUploadResponse> successList = new ArrayList<>();
        List<ImageUploadResponse> failureList = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                if (!isValidImage(file)) {
                    failureList.add(new ImageUploadResponse(
                        false, "无效的图片文件: " + file.getOriginalFilename(), null, null
                    ));
                    continue;
                }

                MediaFile mediaFile = storeFile(file);
                successList.add(new ImageUploadResponse(
                    true, "上传成功", mediaFile.getUrl(), null
                ));

                log.info("图片上传成功: {}", mediaFile.getUrl());

            } catch (Exception e) {
                log.error("图片上传失败: {}", file.getOriginalFilename(), e);
                failureList.add(new ImageUploadResponse(
                    false, "上传失败: " + e.getMessage(), null, null
                ));
            }
        }

        log.info("批量上传完成: 成功 {} 张，失败 {} 张", successList.size(), failureList.size());
        return new BatchUploadResult(successList.size(), failureList.size(), successList, failureList);
    }

    /**
     * 获取图片信息
     */
    public ImageInfo getImageInfo(MultipartFile file) throws IOException {
        // TODO: 实现图片信息提取（宽度、高度等）
        // 这里先返回基本信息
        return ImageInfo.builder()
                .contentType(file.getContentType())
                .size(file.getSize())
                .fileName(file.getOriginalFilename())
                .build();
    }

    /**
     * 删除物理文件
     */
    private boolean deletePhysicalFile(String fileName) {
        try {
            File storageDir = new File(storagePath);
            if (!storageDir.isAbsolute()) {
                String basePath = System.getProperty("user.dir");
                storageDir = new File(basePath, storagePath);
            }

            File fileToDelete = new File(storageDir, fileName);
            if (fileToDelete.exists()) {
                return fileToDelete.delete();
            } else {
                log.warn("物理文件不存在: {}", fileName);
                return true; // 文件不存在也视为删除成功
            }

        } catch (Exception e) {
            log.error("删除物理文件失败: {}", fileName, e);
            return false;
        }
    }

    // ==================== DTO 类 ====================

    /**
     * 图片上传响应
     */
    public static class ImageUploadResponse {
        private boolean success;
        private String message;
        private String url;
        private String thumbnailUrl;

        public ImageUploadResponse(boolean success, String message, String url, String thumbnailUrl) {
            this.success = success;
            this.message = message;
            this.url = url;
            this.thumbnailUrl = thumbnailUrl;
        }

        // Getters and setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
    }

    /**
     * 批量上传结果
     */
    public static class BatchUploadResult {
        private int successCount;
        private int failureCount;
        private List<ImageUploadResponse> successList;
        private List<ImageUploadResponse> failureList;

        public BatchUploadResult(int successCount, int failureCount,
                                List<ImageUploadResponse> successList,
                                List<ImageUploadResponse> failureList) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.successList = successList;
            this.failureList = failureList;
        }

        // Getters and setters
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailureCount() { return failureCount; }
        public void setFailureCount(int failureCount) { this.failureCount = failureCount; }
        public List<ImageUploadResponse> getSuccessList() { return successList; }
        public void setSuccessList(List<ImageUploadResponse> successList) { this.successList = successList; }
        public List<ImageUploadResponse> getFailureList() { return failureList; }
        public void setFailureList(List<ImageUploadResponse> failureList) { this.failureList = failureList; }
    }

    /**
     * 图片信息
     */
    public static class ImageInfo {
        private String contentType;
        private long size;
        private String fileName;
        private Integer width;
        private Integer height;

        public static ImageInfoBuilder builder() {
            return new ImageInfoBuilder();
        }

        // Getters and setters
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public long getSize() { return size; }
        public void setSize(long size) { this.size = size; }
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        public Integer getWidth() { return width; }
        public void setWidth(Integer width) { this.width = width; }
        public Integer getHeight() { return height; }
        public void setHeight(Integer height) { this.height = height; }

        public static class ImageInfoBuilder {
            private ImageInfo imageInfo = new ImageInfo();

            public ImageInfoBuilder contentType(String contentType) {
                imageInfo.setContentType(contentType);
                return this;
            }

            public ImageInfoBuilder size(long size) {
                imageInfo.setSize(size);
                return this;
            }

            public ImageInfoBuilder fileName(String fileName) {
                imageInfo.setFileName(fileName);
                return this;
            }

            public ImageInfoBuilder width(Integer width) {
                imageInfo.setWidth(width);
                return this;
            }

            public ImageInfoBuilder height(Integer height) {
                imageInfo.setHeight(height);
                return this;
            }

            public ImageInfo build() {
                return imageInfo;
            }
        }
    }
}