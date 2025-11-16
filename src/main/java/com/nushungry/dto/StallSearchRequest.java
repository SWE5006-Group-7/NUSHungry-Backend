package com.nushungry.dto;

import lombok.Data;
import java.util.List;

/**
 * 摊位搜索请求DTO
 */
@Data
public class StallSearchRequest {

    /**
     * 关键词搜索（摊位名称、菜系类型）
     */
    private String keyword;

    /**
     * 菜系类型筛选（多选）
     */
    private List<String> cuisineTypes;

    /**
     * 最低评分
     */
    private Double minRating;

    /**
     * 是否只显示Halal食物
     */
    private Boolean halalOnly;

    /**
     * 食堂ID筛选
     */
    private Long cafeteriaId;

    /**
     * 用户位置经度（用于距离排序）
     */
    private Double userLongitude;

    /**
     * 用户位置纬度（用于距离排序）
     */
    private Double userLatitude;

    /**
     * 最大距离（km）
     */
    private Double maxDistance;

    /**
     * 排序方式：rating（评分），distance（距离），reviews（评价数）
     */
    private String sortBy;

    /**
     * 排序方向：asc, desc
     */
    private String sortDirection;

    /**
     * 页码（从0开始）
     */
    private Integer page = 0;

    /**
     * 每页大小
     */
    private Integer size = 20;
}
