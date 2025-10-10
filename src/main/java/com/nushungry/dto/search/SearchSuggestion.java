package com.nushungry.dto.search;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 搜索建议响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchSuggestion {

    /**
     * 搜索建议列表
     */
    private List<Suggestion> suggestions;

    /**
     * 热门搜索关键词
     */
    private List<String> popularKeywords;

    /**
     * 用户最近搜索
     */
    private List<String> recentSearches;

    /**
     * 单个建议项
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Suggestion {
        /**
         * 建议文本
         */
        private String text;

        /**
         * 建议类型: KEYWORD, STALL_NAME, CAFETERIA_NAME, CUISINE_TYPE
         */
        private SuggestionType type;

        /**
         * 匹配的实体ID(如果是摊位或食堂名称)
         */
        private Long entityId;

        /**
         * 权重/相关度分数
         */
        private Double score;
    }

    /**
     * 建议类型枚举
     */
    public enum SuggestionType {
        KEYWORD,        // 关键词
        STALL_NAME,     // 摊位名称
        CAFETERIA_NAME, // 食堂名称
        CUISINE_TYPE    // 菜系类型
    }
}
