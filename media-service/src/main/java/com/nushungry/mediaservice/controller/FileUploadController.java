package com.nushungry.mediaservice.controller;

import com.nushungry.mediaservice.common.ApiResponse;
import com.nushungry.mediaservice.model.MediaFile;
import com.nushungry.mediaservice.service.ImageProcessingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
@Tag(name = "文件上传", description = "文件上传相关接口")
public class FileUploadController {

    private final ImageProcessingService imageProcessingService;

    public FileUploadController(ImageProcessingService imageProcessingService) {
        this.imageProcessingService = imageProcessingService;
    }

    /**
     * 单个图片上传（原有接口保持兼容）
     */
    @PostMapping("/image")
    @Operation(summary = "上传单张图片", description = "通用的图片上传接口，支持验证和存储")
    public ResponseEntity<ApiResponse<MediaFile>> uploadImage(
            @Parameter(description = "图片文件", required = true)
            @RequestParam("file") MultipartFile file) {

        try {
            log.info("接收到图片上传请求: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());

            // 验证是否为有效图片
            if (!imageProcessingService.isValidImage(file)) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("无效的图片文件"));
            }

            MediaFile mediaFile = imageProcessingService.storeFile(file);
            log.info("图片上传成功: {}", mediaFile.getUrl());

            return ResponseEntity.ok(ApiResponse.success(mediaFile));

        } catch (Exception e) {
            log.error("图片上传失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("图片上传失败: " + e.getMessage()));
        }
    }

    /**
     * 批量图片上传
     */
    @PostMapping("/images")
    @Operation(summary = "批量上传图片", description = "一次上传多张图片，返回所有上传成功的图片URL")
    public ResponseEntity<ApiResponse<ImageProcessingService.BatchUploadResult>> uploadImages(
            @Parameter(description = "图片文件列表", required = true)
            @RequestParam("files") List<MultipartFile> files,

            @Parameter(description = "是否生成缩略图", example = "true")
            @RequestParam(value = "generateThumbnail", defaultValue = "true") boolean generateThumbnail,

            @Parameter(description = "是否压缩图片", example = "true")
            @RequestParam(value = "compress", defaultValue = "true") boolean compress) {

        log.info("接收到批量图片上传请求: {} 张图片", files.size());

        try {
            ImageProcessingService.BatchUploadResult result =
                    imageProcessingService.batchUploadImages(files, generateThumbnail, compress);

            log.info("批量上传完成: 成功 {} 张，失败 {} 张",
                    result.getSuccessCount(), result.getFailureCount());

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("批量上传图片失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("批量上传失败: " + e.getMessage()));
        }
    }

    /**
     * 删除图片
     */
    @DeleteMapping("/image")
    @Operation(summary = "删除图片", description = "根据图片URL删除图片文件")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteImage(
            @Parameter(description = "图片URL", required = true, example = "/media/xxx.jpg")
            @RequestParam("url") String imageUrl) {

        log.info("接收到图片删除请求: {}", imageUrl);

        try {
            boolean deleted = imageProcessingService.deleteImageByUrl(imageUrl);

            if (deleted) {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("message", "图片删除成功");

                log.info("图片删除成功: {}", imageUrl);
                return ResponseEntity.ok(ApiResponse.success(responseData));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("图片不存在或删除失败"));
            }

        } catch (Exception e) {
            log.error("图片删除失败: {}", imageUrl, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("图片删除失败: " + e.getMessage()));
        }
    }

    /**
     * 批量删除图片
     */
    @DeleteMapping("/images")
    @Operation(summary = "批量删除图片", description = "根据图片URL列表批量删除图片文件")
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteImages(
            @Parameter(description = "图片URL列表", required = true)
            @RequestBody List<String> imageUrls) {

        log.info("接收到批量图片删除请求: {} 张图片", imageUrls.size());

        try {
            List<String> deletedUrls = imageProcessingService.deleteImagesByUrls(imageUrls);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", String.format("成功删除 %d 张图片", deletedUrls.size()));
            responseData.put("deletedCount", deletedUrls.size());
            responseData.put("deletedFiles", deletedUrls);

            log.info("批量删除完成: 成功 {} 张", deletedUrls.size());
            return ResponseEntity.ok(ApiResponse.success(responseData));

        } catch (Exception e) {
            log.error("批量删除图片失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("批量删除失败: " + e.getMessage()));
        }
    }

    /**
     * 获取图片信息
     */
    @PostMapping("/image/info")
    @Operation(summary = "获取图片信息", description = "获取图片的尺寸、格式、大小等信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getImageInfo(
            @Parameter(description = "图片文件", required = true)
            @RequestParam("file") MultipartFile file) {

        try {
            ImageProcessingService.ImageInfo info = imageProcessingService.getImageInfo(file);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("contentType", info.getContentType());
            responseData.put("size", info.getSize());
            responseData.put("sizeInMB", String.format("%.2f MB", info.getSize() / (1024.0 * 1024.0)));
            responseData.put("fileName", info.getFileName());

            if (info.getWidth() != null) {
                responseData.put("width", info.getWidth());
            }
            if (info.getHeight() != null) {
                responseData.put("height", info.getHeight());
            }

            return ResponseEntity.ok(ApiResponse.success(responseData));

        } catch (Exception e) {
            log.error("获取图片信息失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取图片信息失败: " + e.getMessage()));
        }
    }

    // =================== 兼容原有接口 ===================

    /**
     * 原有的单文件上传接口（保持向后兼容）
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文件（兼容接口）", description = "原有的文件上传接口，保持向后兼容")
    public ResponseEntity<MediaFile> upload(
            @Parameter(description = "文件", required = true)
            @RequestParam("file") MultipartFile file) throws Exception {

        MediaFile saved = imageProcessingService.storeFile(file);
        return ResponseEntity.ok(saved);
    }
}