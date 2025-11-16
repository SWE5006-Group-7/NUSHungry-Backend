package com.nushungry.controller.admin;

import com.nushungry.model.Image;
import com.nushungry.repository.ImageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.persistence.criteria.Predicate;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/images")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class AdminImageController {

    private final ImageRepository imageRepository;

    public AdminImageController(ImageRepository imageRepository) {
        this.imageRepository = imageRepository;
    }

    /**
     * 分页查询图片列表
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllImages(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String uploadedBy,
            @RequestParam(required = false) String keyword) {

        // 创建分页和排序
        Pageable pageable = PageRequest.of(page, size, Sort.by("uploadedAt").descending());

        // 动态构建查询条件
        Specification<Image> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 按类型筛选
            if (type != null && !type.isEmpty()) {
                try {
                    Image.ImageType imageType = Image.ImageType.valueOf(type.toUpperCase());
                    predicates.add(cb.equal(root.get("type"), imageType));
                } catch (IllegalArgumentException e) {
                    // 忽略无效的类型
                }
            }

            // 按上传者筛选
            if (uploadedBy != null && !uploadedBy.isEmpty()) {
                predicates.add(cb.like(root.get("uploadedBy"), "%" + uploadedBy + "%"));
            }

            // 关键词搜索 (搜索图片URL)
            if (keyword != null && !keyword.isEmpty()) {
                predicates.add(cb.or(
                    cb.like(root.get("imageUrl"), "%" + keyword + "%"),
                    cb.like(root.get("uploadedBy"), "%" + keyword + "%")
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 执行查询
        Page<Image> imagePage = imageRepository.findAll(spec, pageable);

        // 构建返回结果
        Map<String, Object> response = new HashMap<>();
        response.put("content", buildImageResponseList(imagePage.getContent()));
        response.put("totalElements", imagePage.getTotalElements());
        response.put("totalPages", imagePage.getTotalPages());
        response.put("currentPage", imagePage.getNumber());
        response.put("pageSize", imagePage.getSize());

        return ResponseEntity.ok(response);
    }

    /**
     * 获取图片统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getImageStats() {
        Map<String, Object> stats = new HashMap<>();

        long totalImages = imageRepository.count();
        long photoCount = imageRepository.count((root, query, cb) ->
            cb.equal(root.get("type"), Image.ImageType.PHOTO));
        long menuCount = imageRepository.count((root, query, cb) ->
            cb.equal(root.get("type"), Image.ImageType.MENU));

        stats.put("totalImages", totalImages);
        stats.put("photoCount", photoCount);
        stats.put("menuCount", menuCount);

        return ResponseEntity.ok(stats);
    }

    /**
     * 删除单张图片
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteImage(@PathVariable Long id) {
        return imageRepository.findById(id)
            .map(image -> {
                // 删除文件系统中的图片文件
                deleteImageFile(image.getImageUrl());
                if (image.getThumbnailUrl() != null) {
                    deleteImageFile(image.getThumbnailUrl());
                }

                // 删除数据库记录
                imageRepository.delete(image);

                Map<String, String> response = new HashMap<>();
                response.put("message", "图片删除成功");
                return ResponseEntity.ok(response);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 批量删除图片
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Map<String, Object>> batchDeleteImages(@RequestBody List<Long> imageIds) {
        int deletedCount = 0;
        int failedCount = 0;

        for (Long id : imageIds) {
            try {
                imageRepository.findById(id).ifPresent(image -> {
                    deleteImageFile(image.getImageUrl());
                    if (image.getThumbnailUrl() != null) {
                        deleteImageFile(image.getThumbnailUrl());
                    }
                    imageRepository.delete(image);
                });
                deletedCount++;
            } catch (Exception e) {
                failedCount++;
                System.err.println("删除图片失败: " + id + ", 错误: " + e.getMessage());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "批量删除完成");
        response.put("deletedCount", deletedCount);
        response.put("failedCount", failedCount);

        return ResponseEntity.ok(response);
    }

    /**
     * 获取图片详情
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getImageDetail(@PathVariable Long id) {
        return imageRepository.findById(id)
            .map(image -> ResponseEntity.ok(buildImageResponse(image)))
            .orElse(ResponseEntity.notFound().build());
    }

    // ========== 私有辅助方法 ==========

    /**
     * 构建图片响应列表
     */
    private List<Map<String, Object>> buildImageResponseList(List<Image> images) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Image image : images) {
            result.add(buildImageResponse(image));
        }
        return result;
    }

    /**
     * 构建单个图片响应对象
     */
    private Map<String, Object> buildImageResponse(Image image) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", image.getId());
        response.put("imageUrl", image.getImageUrl());
        response.put("thumbnailUrl", image.getThumbnailUrl());
        response.put("type", image.getType().name());
        response.put("uploadedAt", image.getUploadedAt());
        response.put("uploadedBy", image.getUploadedBy());

        // 关联的食堂或摊位信息
        if (image.getCafeteria() != null) {
            Map<String, Object> cafeteriaInfo = new HashMap<>();
            cafeteriaInfo.put("id", image.getCafeteria().getId());
            cafeteriaInfo.put("name", image.getCafeteria().getName());
            response.put("cafeteria", cafeteriaInfo);
            response.put("relatedType", "cafeteria");
        }

        if (image.getStall() != null) {
            Map<String, Object> stallInfo = new HashMap<>();
            stallInfo.put("id", image.getStall().getId());
            stallInfo.put("name", image.getStall().getName());
            response.put("stall", stallInfo);
            response.put("relatedType", "stall");
        }

        return response;
    }

    /**
     * 删除文件系统中的图片文件
     */
    private void deleteImageFile(String imageUrl) {
        try {
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // 从URL提取文件路径 (假设格式为 /uploads/xxx.jpg)
                String filePath = "uploads" + imageUrl.substring(imageUrl.lastIndexOf("/"));
                File file = new File(filePath);
                if (file.exists()) {
                    if (file.delete()) {
                        System.out.println("文件删除成功: " + filePath);
                    } else {
                        System.err.println("文件删除失败: " + filePath);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("删除文件时出错: " + e.getMessage());
        }
    }
}
