package com.nushungry.cafeteriaservice.controller;

import com.nushungry.cafeteriaservice.dto.ApiResponse;
import com.nushungry.cafeteriaservice.dto.ImageAssociationRequest;
import com.nushungry.cafeteriaservice.dto.ImageResponse;
import com.nushungry.cafeteriaservice.model.Image;
import com.nushungry.cafeteriaservice.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 摊位图片关联管理控制器
 */
@RestController
@RequestMapping("/api/images/stall/{stallId}")
@Tag(name = "摊位图片管理", description = "管理图片与摊位的关联关系")
public class StallImageController {

    private final ImageService imageService;

    public StallImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    /**
     * 关联图片到摊位
     */
    @PostMapping
    @Operation(summary = "关联图片到摊位", description = "将已上传的图片关联到指定摊位")
    public ResponseEntity<ApiResponse<ImageResponse>> addImageToStall(
            @Parameter(description = "摊位ID") @PathVariable Long stallId,
            @Valid @RequestBody ImageAssociationRequest request) {

        try {
            Image image = imageService.addImageToStall(
                stallId,
                request.getImageUrl(),
                request.getThumbnailUrl(),
                request.getType(),
                request.getUploadedBy()
            );

            ImageResponse response = ImageResponse.fromImage(image);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("图片关联成功", response));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("图片关联失败: " + e.getMessage()));
        }
    }

    /**
     * 获取摊位图片列表
     */
    @GetMapping
    @Operation(summary = "获取摊位图片列表", description = "获取指定摊位的所有图片")
    public ResponseEntity<ApiResponse<List<ImageResponse>>> getStallImages(
            @Parameter(description = "摊位ID") @PathVariable Long stallId,
            @Parameter(description = "图片类型") @RequestParam(required = false) Image.ImageType type) {

        try {
            List<Image> images;
            if (type != null) {
                images = imageService.getStallImagesByType(stallId, type);
            } else {
                images = imageService.getStallImages(stallId);
            }

            List<ImageResponse> responses = images.stream()
                    .map(ImageResponse::fromImage)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(responses));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取图片列表失败: " + e.getMessage()));
        }
    }

    /**
     * 删除摊位图片关联
     */
    @DeleteMapping("/{imageId}")
    @Operation(summary = "删除摊位图片关联", description = "删除图片与摊位的关联关系（不删除实际文件）")
    public ResponseEntity<ApiResponse<Void>> removeStallImage(
            @Parameter(description = "摊位ID") @PathVariable Long stallId,
            @Parameter(description = "图片ID") @PathVariable Long imageId) {

        try {
            imageService.removeStallImage(stallId, imageId);
            return ResponseEntity.ok(ApiResponse.success("图片关联删除成功"));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("删除图片关联失败: " + e.getMessage()));
        }
    }
}