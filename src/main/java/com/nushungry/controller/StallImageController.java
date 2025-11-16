package com.nushungry.controller;

import com.nushungry.model.Image;
import com.nushungry.model.Stall;
import com.nushungry.repository.ImageRepository;
import com.nushungry.repository.StallRepository;
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
 * 摊位图片关联控制器
 * 处理图片与摊位的关联关系
 */
@Slf4j
@RestController
@RequestMapping("/api/stalls")
@CrossOrigin(origins = "*")
@Tag(name = "摊位图片管理", description = "摊位图片关联相关接口")
public class StallImageController {

    private final StallRepository stallRepository;
    private final ImageRepository imageRepository;

    public StallImageController(StallRepository stallRepository,
                               ImageRepository imageRepository) {
        this.stallRepository = stallRepository;
        this.imageRepository = imageRepository;
    }

    /**
     * 关联图片到摊位
     */
    @PostMapping("/{stallId}/images")
    @Operation(summary = "关联图片到摊位", description = "将上传的图片URL关联到指定摊位")
    public ResponseEntity<Map<String, Object>> linkImagesToStall(
            @Parameter(description = "摊位ID", required = true)
            @PathVariable Long stallId,

            @Parameter(description = "图片URL列表", required = true)
            @RequestBody LinkImagesRequest request) {

        log.info("接收到关联图片请求: stallId={}, imageUrls={}, type={}", stallId, request.getImageUrls(), request.getType());

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证摊位是否存在
            Stall stall = stallRepository.findById(stallId)
                    .orElseThrow(() -> new RuntimeException("摊位不存在: ID=" + stallId));

            // 获取当前用户ID
            String uploadedBy = getCurrentUserId();

            // 默认图片类型为PHOTO
            Image.ImageType imageType = request.getType() != null ?
                request.getType() : Image.ImageType.PHOTO;

            // 创建图片实体并关联到摊位
            List<Image> savedImages = new ArrayList<>();
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                String imageUrl = request.getImageUrls().get(i);

                Image image = new Image();
                image.setImageUrl(imageUrl);
                image.setUploadedBy(uploadedBy);
                image.setStall(stall);
                image.setType(imageType);
                image.setUploadedAt(LocalDateTime.now());

                // 如果有缩略图URL，也保存
                if (request.getThumbnailUrls() != null &&
                    i < request.getThumbnailUrls().size()) {
                    image.setThumbnailUrl(request.getThumbnailUrls().get(i));
                }

                Image savedImage = imageRepository.save(image);
                savedImages.add(savedImage);
                log.info("图片已关联到摊位: imageId={}, stallId={}, type={}",
                    savedImage.getId(), stallId, imageType);
            }

            response.put("success", true);
            response.put("message", String.format("成功关联 %d 张图片到摊位", savedImages.size()));
            response.put("count", savedImages.size());
            response.put("images", savedImages);

            return ResponseEntity.ok(response);

        } catch (RuntimeException ex) {
            log.error("关联图片到摊位失败: stallId={}", stallId, ex);
            response.put("success", false);
            response.put("message", "关联失败: " + ex.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception ex) {
            log.error("关联图片到摊位时发生错误: stallId={}", stallId, ex);
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
    @DeleteMapping("/{stallId}/images/{imageId}")
    @Operation(summary = "删除摊位图片", description = "删除指定摊位的图片")
    public ResponseEntity<Map<String, Object>> unlinkImageFromStall(
            @Parameter(description = "摊位ID", required = true)
            @PathVariable Long stallId,

            @Parameter(description = "图片ID", required = true)
            @PathVariable Long imageId) {

        log.info("接收到删除摊位图片请求: stallId={}, imageId={}", stallId, imageId);

        Map<String, Object> response = new HashMap<>();

        try {
            // 验证图片是否存在且属于该摊位
            Image image = imageRepository.findById(imageId)
                    .orElseThrow(() -> new RuntimeException("图片不存在: ID=" + imageId));

            if (image.getStall() == null || !image.getStall().getId().equals(stallId)) {
                throw new RuntimeException("该图片不属于指定摊位");
            }

            // 删除图片记录
            imageRepository.delete(image);

            response.put("success", true);
            response.put("message", "图片删除成功");

            log.info("摊位图片已删除: imageId={}, stallId={}", imageId, stallId);

            return ResponseEntity.ok(response);

        } catch (RuntimeException ex) {
            log.error("删除摊位图片失败: stallId={}, imageId={}", stallId, imageId, ex);
            response.put("success", false);
            response.put("message", "删除失败: " + ex.getMessage());
            return ResponseEntity.badRequest().body(response);

        } catch (Exception ex) {
            log.error("删除摊位图片时发生错误: stallId={}, imageId={}", stallId, imageId, ex);
            response.put("success", false);
            response.put("message", "删除失败: " + ex.getMessage());
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response);
        }
    }

    /**
     * 获取摊位的所有图片
     */
    @GetMapping("/{stallId}/images")
    @Operation(summary = "获取摊位图片列表", description = "获取指定摊位的所有图片，可按类型过滤")
    public ResponseEntity<List<Image>> getStallImages(
            @Parameter(description = "摊位ID", required = true)
            @PathVariable Long stallId,

            @Parameter(description = "图片类型 (PHOTO/MENU)", required = false)
            @RequestParam(required = false) String type) {

        log.info("获取摊位图片列表: stallId={}, type={}", stallId, type);

        try {
            List<Image> images = imageRepository.findByStallId(stallId);

            // 如果指定了类型，进行过滤
            if (type != null) {
                try {
                    Image.ImageType imageType = Image.ImageType.valueOf(type.toUpperCase());
                    images = images.stream()
                        .filter(img -> img.getType() == imageType)
                        .toList();
                } catch (IllegalArgumentException e) {
                    log.warn("无效的图片类型: {}", type);
                }
            }

            log.info("摊位图片列表获取成功: stallId={}, type={}, count={}", stallId, type, images.size());
            return ResponseEntity.ok(images);

        } catch (Exception ex) {
            log.error("获取摊位图片列表失败: stallId={}", stallId, ex);
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
        private Image.ImageType type;  // 图片类型：PHOTO 或 MENU
    }
}
