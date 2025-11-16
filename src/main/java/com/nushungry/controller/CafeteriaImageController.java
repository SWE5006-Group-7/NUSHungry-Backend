package com.nushungry.controller;

import com.nushungry.model.Cafeteria;
import com.nushungry.model.Image;
import com.nushungry.repository.CafeteriaRepository;
import com.nushungry.repository.ImageRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 食堂图片关联控制器
 * 处理图片与食堂的关联关系
 */
@Slf4j
@RestController
@RequestMapping("/api/cafeterias")
@CrossOrigin(origins = "*")
@Tag(name = "食堂图片管理", description = "食堂图片关联相关接口")
public class CafeteriaImageController {

    private final CafeteriaRepository cafeteriaRepository;
    private final ImageRepository imageRepository;

    public CafeteriaImageController(CafeteriaRepository cafeteriaRepository,
                                   ImageRepository imageRepository) {
        this.cafeteriaRepository = cafeteriaRepository;
        this.imageRepository = imageRepository;
    }

    /**
     * 关联图片到食堂
     */
    @PostMapping("/{cafeteriaId}/images")
    @Operation(summary = "关联图片到食堂", description = "将上传的图片URL关联到指定食堂")
    public ResponseEntity<Map<String, Object>> linkImagesToCafeteria(
            @Parameter(description = "食堂ID", required = true)
            @PathVariable Long cafeteriaId,

            @Parameter(description = "图片URL列表", required = true)
            @RequestBody LinkImagesRequest request) {

        log.info("接收到关联图片请求: cafeteriaId={}, imageUrls={}", cafeteriaId, request.getImageUrls());

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证食堂是否存在
            Cafeteria cafeteria = cafeteriaRepository.findById(cafeteriaId)
                    .orElseThrow(() -> new RuntimeException("食堂不存在: ID=" + cafeteriaId));

            // 获取当前用户ID
            String uploadedBy = getCurrentUserId();

            // 创建图片实体并关联到食堂
            List<Image> savedImages = new ArrayList<>();
            for (String imageUrl : request.getImageUrls()) {
                Image image = new Image();
                image.setImageUrl(imageUrl);
                image.setUploadedBy(uploadedBy);
                image.setCafeteria(cafeteria);
                image.setUploadedAt(LocalDateTime.now());

                // 如果有缩略图URL，也保存
                if (request.getThumbnailUrls() != null &&
                    request.getThumbnailUrls().size() > request.getImageUrls().indexOf(imageUrl)) {
                    String thumbnailUrl = request.getThumbnailUrls().get(request.getImageUrls().indexOf(imageUrl));
                    // 注意：需要在 Image 实体中添加 thumbnailUrl 字段
                    // image.setThumbnailUrl(thumbnailUrl);
                }

                Image savedImage = imageRepository.save(image);
                savedImages.add(savedImage);
                log.info("图片已关联到食堂: imageId={}, cafeteriaId={}", savedImage.getId(), cafeteriaId);
            }

            response.put("success", true);
            response.put("message", String.format("成功关联 %d 张图片到食堂", savedImages.size()));
            response.put("count", savedImages.size());
            response.put("images", savedImages);

            return ResponseEntity.ok(response);

        } catch (RuntimeException ex) {
            log.error("关联图片到食堂失败: cafeteriaId={}", cafeteriaId, ex);
            response.put("success", false);
            response.put("message", "关联失败: " + ex.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception ex) {
            log.error("关联图片到食堂时发生错误: cafeteriaId={}", cafeteriaId, ex);
            response.put("success", false);
            response.put("message", "关联失败: " + ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    /**
     * 取消关联图片（删除图片）
     */
    @DeleteMapping("/{cafeteriaId}/images/{imageId}")
    @Operation(summary = "删除食堂图片", description = "删除指定食堂的图片")
    public ResponseEntity<Map<String, Object>> unlinkImageFromCafeteria(
            @Parameter(description = "食堂ID", required = true)
            @PathVariable Long cafeteriaId,

            @Parameter(description = "图片ID", required = true)
            @PathVariable Long imageId) {

        log.info("接收到删除食堂图片请求: cafeteriaId={}, imageId={}", cafeteriaId, imageId);

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证图片是否存在且属于该食堂
            Image image = imageRepository.findById(imageId)
                    .orElseThrow(() -> new RuntimeException("图片不存在: ID=" + imageId));

            if (image.getCafeteria() == null || !image.getCafeteria().getId().equals(cafeteriaId)) {
                throw new RuntimeException("该图片不属于指定食堂");
            }

            // 删除图片记录
            imageRepository.delete(image);

            response.put("success", true);
            response.put("message", "图片删除成功");

            log.info("食堂图片已删除: imageId={}, cafeteriaId={}", imageId, cafeteriaId);

            return ResponseEntity.ok(response);

        } catch (RuntimeException ex) {
            log.error("删除食堂图片失败: cafeteriaId={}, imageId={}", cafeteriaId, imageId, ex);
            response.put("success", false);
            response.put("message", "删除失败: " + ex.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception ex) {
            log.error("删除食堂图片时发生错误: cafeteriaId={}, imageId={}", cafeteriaId, imageId, ex);
            response.put("success", false);
            response.put("message", "删除失败: " + ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    /**
     * 获取食堂的所有图片
     */
    @GetMapping("/{cafeteriaId}/images")
    @Operation(summary = "获取食堂图片列表", description = "获取指定食堂的所有图片")
    public ResponseEntity<List<Image>> getCafeteriaImages(
            @Parameter(description = "食堂ID", required = true)
            @PathVariable Long cafeteriaId) {

        log.info("获取食堂图片列表: cafeteriaId={}", cafeteriaId);

        try {
            List<Image> images = imageRepository.findByCafeteriaId(cafeteriaId);
            log.info("食堂图片列表获取成功: cafeteriaId={}, count={}", cafeteriaId, images.size());
            return ResponseEntity.ok(images);

        } catch (Exception ex) {
            log.error("获取食堂图片列表失败: cafeteriaId={}", cafeteriaId, ex);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ArrayList<>());
        }
    }

    /**
     * 获取当前登录用户ID
     */
    private String getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                return authentication.getName();
            }
        } catch (Exception ex) {
            log.warn("获取当前用户ID失败", ex);
        }
        // 如果获取失败，返回临时ID
        return "guest-" + System.currentTimeMillis();
    }

    /**
     * 关联图片请求DTO
     */
    @Data
    public static class LinkImagesRequest {
        private List<String> imageUrls;
        private List<String> thumbnailUrls;
    }
}
