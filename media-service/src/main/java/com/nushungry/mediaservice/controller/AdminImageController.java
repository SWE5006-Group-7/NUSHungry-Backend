package com.nushungry.mediaservice.controller;

import com.nushungry.mediaservice.common.ApiResponse;
import com.nushungry.mediaservice.dto.ImageDetailResponse;
import com.nushungry.mediaservice.dto.ImageStatsResponse;
import com.nushungry.mediaservice.model.MediaFile;
import com.nushungry.mediaservice.service.AdminImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/admin/images")
@RequiredArgsConstructor
@Tag(name = "管理员图片管理", description = "管理员图片管理相关接口")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
@Slf4j
public class AdminImageController {

    private final AdminImageService adminImageService;

    @GetMapping
    @Operation(summary = "分页查询图片列表", description = "管理员分页查询图片列表，支持按类型、上传者、关键词筛选")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String uploadedBy,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection
    ) {
        log.info("Admin getting images with filters - page: {}, size: {}, type: {}, uploadedBy: {}, keyword: {}",
                page, size, type, uploadedBy, keyword);

        Sort.Direction direction = sortDirection.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<MediaFile> imagePage = adminImageService.getAllImages(type, uploadedBy, keyword, pageable);

        Map<String, Object> response = new HashMap<>();
        response.put("images", imagePage.getContent());
        response.put("currentPage", imagePage.getNumber());
        response.put("totalItems", imagePage.getTotalElements());
        response.put("totalPages", imagePage.getTotalPages());
        response.put("pageSize", imagePage.getSize());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stats")
    @Operation(summary = "获取图片统计信息", description = "获取图片统计信息，包括总数、总大小、类型分布、上传者分布等")
    public ResponseEntity<ApiResponse<ImageStatsResponse>> getImageStats() {
        log.info("Admin getting image statistics");

        ImageStatsResponse stats = adminImageService.getImageStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除单张图片", description = "管理员删除指定图片，同时删除文件和数据库记录")
    public ResponseEntity<ApiResponse<String>> deleteImage(@PathVariable Long id) {
        log.info("Admin deleting image ID: {}", id);

        boolean deleted = adminImageService.deleteImage(id);
        if (deleted) {
            return ResponseEntity.ok(ApiResponse.success("图片删除成功"));
        } else {
            return ResponseEntity.ok(ApiResponse.error("图片不存在或删除失败"));
        }
    }

    @DeleteMapping("/batch")
    @Operation(summary = "批量删除图片", description = "管理员批量删除图片，同时删除文件和数据库记录")
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchDeleteImages(
            @RequestBody List<Long> imageIds
    ) {
        log.info("Admin batch deleting {} images", imageIds.size());

        Map<String, Object> result = adminImageService.batchDeleteImages(imageIds);

        String message = String.format("批量删除完成：成功 %d 个，失败 %d 个",
                result.get("deletedCount"), result.get("failedCount"));

        return ResponseEntity.ok(ApiResponse.success(message, result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取图片详情", description = "管理员获取指定图片的详细信息")
    public ResponseEntity<ApiResponse<ImageDetailResponse>> getImageDetail(@PathVariable Long id) {
        log.info("Admin getting image detail ID: {}", id);

        Optional<ImageDetailResponse> imageDetail = adminImageService.getImageDetail(id);
        if (imageDetail.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(imageDetail.get()));
        } else {
            return ResponseEntity.ok(ApiResponse.error("图片不存在"));
        }
    }
}