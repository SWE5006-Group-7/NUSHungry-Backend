package com.nushungry.dto.search;

import com.nushungry.model.SearchHistory;
import lombok.Data;

import java.util.List;

/**
 * 搜索请求参数
 */
@Data
public class SearchRequest {

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 搜索类型: STALL, CAFETERIA, ALL
     */
    private SearchHistory.SearchType searchType = SearchHistory.SearchType.ALL;

    /**
     * 菜系类型筛选(可多选)
     */
    private List<String> cuisineTypes;

    /**
     * Halal筛选
     */
    private Boolean halalOnly;

    /**
     * 最低评分筛选
     */
    private Double minRating;

    /**
     * 最高评分筛选
     */
    private Double maxRating;

    /**
     * 食堂ID筛选(搜索特定食堂内的摊位)
     */
    private Long cafeteriaId;

    /**
     * 排序方式: RELEVANCE(相关度), RATING(评分), REVIEW_COUNT(评价数), NAME(名称)
     */
    private SortBy sortBy = SortBy.RELEVANCE;

    /**
     * 排序方向: ASC, DESC
     */
    private SortDirection sortDirection = SortDirection.DESC;

    /**
     * 页码(从0开始)
     */
    private Integer page = 0;

    /**
     * 每页数量
     */
    private Integer size = 20;

    /**
     * 排序方式枚举
     */
    public enum SortBy {
        RELEVANCE,      // 相关度
        RATING,         // 评分
        REVIEW_COUNT,   // 评价数
        NAME,           // 名称
        DISTANCE        // 距离(未来实现)
    }

    /**
     * 排序方向枚举
     */
    public enum SortDirection {
        ASC,
        DESC
    }
}
