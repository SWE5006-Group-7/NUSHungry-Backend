package com.nushungry.dto.search;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.util.List;

/**
 * 搜索结果响应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SearchResult<T> {

    /**
     * 搜索结果列表
     */
    private List<T> results;

    /**
     * 总结果数
     */
    private long totalCount;

    /**
     * 当前页码
     */
    private int currentPage;

    /**
     * 每页数量
     */
    private int pageSize;

    /**
     * 总页数
     */
    private int totalPages;

    /**
     * 搜索关键词
     */
    private String keyword;

    /**
     * 搜索耗时(毫秒)
     */
    private Long searchTime;

    /**
     * 是否有下一页
     */
    public boolean hasNext() {
        return currentPage < totalPages - 1;
    }

    /**
     * 是否有上一页
     */
    public boolean hasPrevious() {
        return currentPage > 0;
    }
}
