package com.nushungry.mediaservice.service;

import com.nushungry.mediaservice.dto.ImageDetailResponse;
import com.nushungry.mediaservice.dto.ImageStatsResponse;
import com.nushungry.mediaservice.model.MediaFile;
import com.nushungry.mediaservice.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminImageService {

    private final MediaFileRepository mediaFileRepository;

    /**
     * 分页查询图片列表
     */
    public Page<MediaFile> getAllImages(String type, String uploadedBy, String keyword, Pageable pageable) {
        Specification<MediaFile> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 按类型筛选
            if (type != null && !type.trim().isEmpty()) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            // 按上传者筛选
            if (uploadedBy != null && !uploadedBy.trim().isEmpty()) {
                predicates.add(cb.like(root.get("uploadedBy"), "%" + uploadedBy + "%"));
            }

            // 关键词搜索
            if (keyword != null && !keyword.trim().isEmpty()) {
                predicates.add(cb.or(
                    cb.like(root.get("fileName"), "%" + keyword + "%"),
                    cb.like(root.get("originalFileName"), "%" + keyword + "%"),
                    cb.like(root.get("uploadedBy"), "%" + keyword + "%")
                ));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return mediaFileRepository.findAll(spec, pageable);
    }

    /**
     * 获取图片统计信息
     */
    public ImageStatsResponse getImageStats() {
        // 基础统计
        long totalImages = mediaFileRepository.count();
        Long totalSize = mediaFileRepository.getTotalSize() != null ? mediaFileRepository.getTotalSize() : 0L;

        // 类型分布
        Map<String, Long> typeDistribution = new HashMap<>();
        List<Object[]> typeStats = mediaFileRepository.countByTypeGroupBy();
        for (Object[] stat : typeStats) {
            typeDistribution.put((String) stat[0], (Long) stat[1]);
        }

        // 上传者分布
        Map<String, Long> uploadByDistribution = new HashMap<>();
        List<Object[]> uploadStats = mediaFileRepository.countByUploadedByGroupBy();
        for (Object[] stat : uploadStats) {
            uploadByDistribution.put((String) stat[0], (Long) stat[1]);
        }

        // 时间统计
        LocalDateTime now = LocalDateTime.now();
        long todayCount = mediaFileRepository.countByCreatedAtBetween(
                now.toLocalDate().atStartOfDay(),
                now
        );
        long thisWeekCount = mediaFileRepository.countByCreatedAtBetween(
                now.minusDays(7),
                now
        );
        long thisMonthCount = mediaFileRepository.countByCreatedAtBetween(
                now.minusDays(30),
                now
        );

        return ImageStatsResponse.builder()
                .totalImages(totalImages)
                .totalSize(totalSize)
                .typeDistribution(typeDistribution)
                .uploadByDistribution(uploadByDistribution)
                .todayCount(todayCount)
                .thisWeekCount(thisWeekCount)
                .thisMonthCount(thisMonthCount)
                .build();
    }

    /**
     * 获取图片详情
     */
    public Optional<ImageDetailResponse> getImageDetail(Long id) {
        return mediaFileRepository.findById(id)
                .map(this::buildImageDetailResponse);
    }

    /**
     * 删除图片（同时删除文件和数据库记录）
     */
    @Transactional
    public boolean deleteImage(Long id) {
        Optional<MediaFile> mediaFile = mediaFileRepository.findById(id);
        if (mediaFile.isPresent()) {
            MediaFile file = mediaFile.get();

            // 删除文件系统中的图片
            deleteImageFile(file.getUrl());
            if (file.getThumbnailUrl() != null) {
                deleteImageFile(file.getThumbnailUrl());
            }

            // 删除数据库记录
            mediaFileRepository.delete(file);
            log.info("图片删除成功: ID={}, 文件名={}", id, file.getFileName());
            return true;
        }
        return false;
    }

    /**
     * 批量删除图片
     */
    @Transactional
    public Map<String, Object> batchDeleteImages(List<Long> ids) {
        int deletedCount = 0;
        int failedCount = 0;
        List<String> failedIds = new ArrayList<>();

        for (Long id : ids) {
            try {
                if (deleteImage(id)) {
                    deletedCount++;
                } else {
                    failedCount++;
                    failedIds.add(id.toString());
                }
            } catch (Exception e) {
                failedCount++;
                failedIds.add(id.toString());
                log.error("删除图片失败: ID={}, 错误: {}", id, e.getMessage());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("deletedCount", deletedCount);
        result.put("failedCount", failedCount);
        result.put("failedIds", failedIds);

        log.info("批量删除完成: 成功={}, 失败={}", deletedCount, failedCount);
        return result;
    }

    /**
     * 构建图片详情响应
     */
    private ImageDetailResponse buildImageDetailResponse(MediaFile mediaFile) {
        return ImageDetailResponse.builder()
                .id(mediaFile.getId())
                .fileName(mediaFile.getFileName())
                .originalFileName(mediaFile.getOriginalFileName())
                .url(mediaFile.getUrl())
                .thumbnailUrl(mediaFile.getThumbnailUrl())
                .contentType(mediaFile.getContentType())
                .size(mediaFile.getSize())
                .uploadedBy(mediaFile.getUploadedBy())
                .type(mediaFile.getType())
                .relatedId(mediaFile.getRelatedId())
                .relatedType(mediaFile.getRelatedType())
                .createdAt(mediaFile.getCreatedAt())
                .updatedAt(mediaFile.getUpdatedAt())
                .build();
    }

    /**
     * 删除文件系统中的图片文件
     */
    private void deleteImageFile(String imageUrl) {
        try {
            if (imageUrl != null && !imageUrl.trim().isEmpty()) {
                // 从URL提取文件路径 (假设格式为 /uploads/xxx.jpg)
                String filePath = "uploads" + imageUrl.substring(imageUrl.lastIndexOf("/"));
                File file = new File(filePath);
                if (file.exists()) {
                    if (file.delete()) {
                        log.info("文件删除成功: {}", filePath);
                    } else {
                        log.warn("文件删除失败: {}", filePath);
                    }
                }
            }
        } catch (Exception e) {
            log.error("删除文件时出错: URL={}, 错误: {}", imageUrl, e.getMessage());
        }
    }
}