package com.nushungry.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 评价响应 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long stallId;
    private String stallName;
    private Long userId;
    private String username;
    private String userAvatarUrl;
    private Double rating;
    private String comment;
    private List<String> imageUrls;

    // 多维度评分
    private Double tasteRating;
    private Double environmentRating;
    private Double serviceRating;
    private Double valueRating;

    // 互动数据
    private Integer likeCount; // 点赞数

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean canEdit; // 当前用户是否可以编辑
    private boolean canDelete; // 当前用户是否可以删除
}
