package com.nushungry.controller;

import com.nushungry.service.FileStorageService;
import com.nushungry.service.ImageProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文件上传控制器
 * 提供通用的图片上传、批量上传、删除等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/upload")
@CrossOrigin(origins = "*")
@Tag(name = "文件上传", description = "文件上传相关接口")
public class FileUploadController {

    private final FileStorageService fileStorageService;
    private final ImageProcessingService imageProcessingService;

    public FileUploadController(FileStorageService fileStorageService,
                               ImageProcessingService imageProcessingService) {
        this.fileStorageService = fileStorageService;
        this.imageProcessingService = imageProcessingService;
    }

    /**
     * 单个图片上传（通用接口）
     */
    @PostMapping("/image")
    @Operation(summary = "上传单张图片", description = "通用的图片上传接口，支持压缩和缩略图生成")
    public ResponseEntity<ImageUploadResponse> uploadImage(
            @Parameter(description = "图片文件", required = true)
            @RequestParam("file") MultipartFile file,

            @Parameter(description = "是否生成缩略图", example = "true")
            @RequestParam(value = "generateThumbnail", defaultValue = "true") boolean generateThumbnail,

            @Parameter(description = "是否压缩图片", example = "true")
            @RequestParam(value = "compress", defaultValue = "true") boolean compress) {

        try {
            log.info("接收到图片上传请求: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

            // 验证是否为有效图片
            if (!imageProcessingService.isValidImage(file)) {
                return ResponseEntity
                    .badRequest()
                    .body(new ImageUploadResponse(false, "无效的图片文件", null, null));
            }

            String originalUrl;
            String thumbnailUrl = null;

            if (compress) {
                // 压缩并生成缩略图
                ImageProcessingService.ImageProcessingResult result =
                    imageProcessingService.processImage(file, generateThumbnail);
                originalUrl = result.getOriginalUrl();
                thumbnailUrl = result.getThumbnailUrl();
            } else {
                // 直接保存原图
                originalUrl = fileStorageService.storeFile(file, true);
            }

            log.info("图片上传成功: {}", originalUrl);

            return ResponseEntity.ok(new ImageUploadResponse(
                true,
                "图片上传成功",
                originalUrl,
                thumbnailUrl
            ));

        } catch (IllegalArgumentException ex) {
            log.warn("图片上传失败: {}", ex.getMessage());
            return ResponseEntity
                .badRequest()
                .body(new ImageUploadResponse(false, ex.getMessage(), null, null));

        } catch (IOException ex) {
            log.error("图片上传失败", ex);
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ImageUploadResponse(false, "图片上传失败: " + ex.getMessage(), null, null));
        }
    }

    /**
     * 批量图片上传
     */
    @PostMapping("/images")
    @Operation(summary = "批量上传图片", description = "一次上传多张图片，返回所有上传成功的图片URL")
    public ResponseEntity<BatchUploadResponse> uploadImages(
            @Parameter(description = "图片文件列表", required = true)
            @RequestParam("files") List<MultipartFile> files,

            @Parameter(description = "是否生成缩略图", example = "true")
            @RequestParam(value = "generateThumbnail", defaultValue = "true") boolean generateThumbnail,

            @Parameter(description = "是否压缩图片", example = "true")
            @RequestParam(value = "compress", defaultValue = "true") boolean compress) {

        log.info("接收到批量图片上传请求: {} 张图片", files.size());

        List<ImageUploadResponse> successList = new ArrayList<>();
        List<ImageUploadResponse> failureList = new ArrayList<>();

        for (MultipartFile file : files) {
            try {
                // 验证是否为有效图片
                if (!imageProcessingService.isValidImage(file)) {
                    failureList.add(new ImageUploadResponse(
                        false,
                        "无效的图片文件: " + file.getOriginalFilename(),
                        null,
                        null
                    ));
                    continue;
                }

                String originalUrl;
                String thumbnailUrl = null;

                if (compress) {
                    ImageProcessingService.ImageProcessingResult result =
                        imageProcessingService.processImage(file, generateThumbnail);
                    originalUrl = result.getOriginalUrl();
                    thumbnailUrl = result.getThumbnailUrl();
                } else {
                    originalUrl = fileStorageService.storeFile(file, true);
                }

                successList.add(new ImageUploadResponse(
                    true,
                    "上传成功",
                    originalUrl,
                    thumbnailUrl
                ));

                log.info("图片上传成功: {}", originalUrl);

            } catch (Exception ex) {
                log.error("图片上传失败: {}", file.getOriginalFilename(), ex);
                failureList.add(new ImageUploadResponse(
                    false,
                    "上传失败: " + ex.getMessage(),
                    null,
                    null
                ));
            }
        }

        log.info("批量上传完成: 成功 {} 张，失败 {} 张", successList.size(), failureList.size());

        return ResponseEntity.ok(new BatchUploadResponse(
            successList.size(),
            failureList.size(),
            successList,
            failureList
        ));
    }

    /**
     * 删除图片
     */
    @DeleteMapping("/image")
    @Operation(summary = "删除图片", description = "根据图片URL删除图片文件")
    public ResponseEntity<Map<String, Object>> deleteImage(
            @Parameter(description = "图片URL", required = true, example = "/uploads/images/2024/01/15/xxx.jpg")
            @RequestParam("url") String imageUrl) {

        log.info("接收到图片删除请求: {}", imageUrl);

        Map<String, Object> response = new HashMap<>();

        try {
            boolean deleted = fileStorageService.deleteFile(imageUrl);

            if (deleted) {
                response.put("success", true);
                response.put("message", "图片删除成功");
                log.info("图片删除成功: {}", imageUrl);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "图片不存在或删除失败");
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception ex) {
            log.error("图片删除失败: {}", imageUrl, ex);
            response.put("success", false);
            response.put("message", "图片删除失败: " + ex.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
        }
    }

    /**
     * 批量删除图片
     */
    @DeleteMapping("/images")
    @Operation(summary = "批量删除图片", description = "根据图片URL列表批量删除图片文件")
    public ResponseEntity<Map<String, Object>> deleteImages(
            @Parameter(description = "图片URL列表", required = true)
            @RequestBody List<String> imageUrls) {

        log.info("接收到批量图片删除请求: {} 张图片", imageUrls.size());

        Map<String, Object> response = new HashMap<>();

        try {
            List<String> deletedFiles = fileStorageService.deleteFiles(imageUrls);

            response.put("success", true);
            response.put("message", String.format("成功删除 %d 张图片", deletedFiles.size()));
            response.put("deletedCount", deletedFiles.size());
            response.put("deletedFiles", deletedFiles);

            log.info("批量删除完成: 成功 {} 张", deletedFiles.size());

            return ResponseEntity.ok(response);

        } catch (Exception ex) {
            log.error("批量删除图片失败", ex);
            response.put("success", false);
            response.put("message", "批量删除失败: " + ex.getMessage());
            return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
        }
    }

    /**
     * 获取图片信息
     */
    @PostMapping("/image/info")
    @Operation(summary = "获取图片信息", description = "获取图片的尺寸、格式、大小等信息")
    public ResponseEntity<Map<String, Object>> getImageInfo(
            @Parameter(description = "图片文件", required = true)
            @RequestParam("file") MultipartFile file) {

        Map<String, Object> response = new HashMap<>();

        try {
            ImageProcessingService.ImageInfo info = imageProcessingService.getImageInfo(file);

            response.put("success", true);
            response.put("width", info.getWidth());
            response.put("height", info.getHeight());
            response.put("contentType", info.getContentType());
            response.put("size", info.getSize());
            response.put("sizeInMB", String.format("%.2f MB", info.getSize() / (1024.0 * 1024.0)));

            return ResponseEntity.ok(response);

        } catch (IOException ex) {
            log.error("获取图片信息失败", ex);
            response.put("success", false);
            response.put("message", "获取图片信息失败: " + ex.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== 响应DTO ====================

    /**
     * 单个图片上传响应
     */
    @Data
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
    }

    /**
     * 批量上传响应
     */
    @Data
    public static class BatchUploadResponse {
        private int successCount;
        private int failureCount;
        private List<ImageUploadResponse> successList;
        private List<ImageUploadResponse> failureList;

        public BatchUploadResponse(int successCount, int failureCount,
                                  List<ImageUploadResponse> successList,
                                  List<ImageUploadResponse> failureList) {
            this.successCount = successCount;
            this.failureCount = failureCount;
            this.successList = successList;
            this.failureList = failureList;
        }
    }
}
