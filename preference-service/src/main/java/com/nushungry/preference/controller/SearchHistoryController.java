package com.nushungry.preference.controller;

import com.nushungry.preference.entity.SearchHistory;
import com.nushungry.preference.service.SearchHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索历史控制器
 */
@RestController
@RequestMapping("/api/search-history")
@Tag(name = "搜索历史管理", description = "用户搜索历史记录的增删改查接口")
@SecurityRequirement(name = "Bearer Authentication")
public class SearchHistoryController {

    @Autowired
    private SearchHistoryService searchHistoryService;

    /**
     * 获取当前用户的搜索历史（分页）
     * GET /api/search-history?page=0&size=20
     */
    @Operation(summary = "获取用户搜索历史", description = "获取当前用户的搜索历史记录，支持分页")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserSearchHistory(
        HttpServletRequest request,
        @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
        @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size
    ) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        Page<SearchHistory> pageResult = searchHistoryService.getUserSearchHistory(userId, page, size);

        Map<String, Object> response = new HashMap<>();
        response.put("content", pageResult.getContent());
        response.put("totalElements", pageResult.getTotalElements());
        response.put("totalPages", pageResult.getTotalPages());
        response.put("currentPage", pageResult.getNumber());

        return ResponseEntity.ok(response);
    }

    /**
     * 获取当前用户最近的搜索历史（限制数量）
     * GET /api/search-history/recent?limit=10
     */
    @Operation(summary = "获取最近搜索历史", description = "获取当前用户最近的搜索历史记录")
    @GetMapping("/recent")
    public ResponseEntity<List<SearchHistory>> getRecentSearches(
        HttpServletRequest request,
        @Parameter(description = "限制数量，最大50") @RequestParam(defaultValue = "10") int limit
    ) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        List<SearchHistory> history = searchHistoryService.getUserRecentSearches(userId, limit);
        return ResponseEntity.ok(history);
    }

    /**
     * 获取当前用户最近的搜索关键词（去重）
     * GET /api/search-history/keywords?limit=10
     */
    @Operation(summary = "获取最近搜索关键词", description = "获取当前用户最近的唯一搜索关键词，已去重")
    @GetMapping("/keywords")
    public ResponseEntity<List<String>> getRecentKeywords(
        HttpServletRequest request,
        @Parameter(description = "限制数量，最大50") @RequestParam(defaultValue = "10") int limit
    ) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            // 未登录用户返回空数组
            return ResponseEntity.ok(java.util.Collections.emptyList());
        }

        List<String> keywords = searchHistoryService.getUserRecentKeywords(userId, limit);
        return ResponseEntity.ok(keywords);
    }

    /**
     * 获取热门搜索关键词
     * GET /api/search-history/popular?days=7&limit=10
     */
    @Operation(summary = "获取热门搜索关键词", description = "获取指定天数内的热门搜索关键词")
    @GetMapping("/popular")
    public ResponseEntity<List<Map<String, Object>>> getPopularKeywords(
        @Parameter(description = "统计天数") @RequestParam(defaultValue = "7") int days,
        @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") int limit
    ) {
        List<Map<String, Object>> keywords = searchHistoryService.getPopularKeywords(days, limit);
        return ResponseEntity.ok(keywords);
    }

    /**
     * 删除当前用户的单条搜索历史
     * DELETE /api/search-history/{id}
     */
    @Operation(summary = "删除单条搜索历史", description = "删除指定的单条搜索历史记录")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSearchHistory(
        HttpServletRequest request,
        @Parameter(description = "搜索历史ID") @PathVariable Long id
    ) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        searchHistoryService.deleteSearchHistory(id, userId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Search history deleted successfully");
        return ResponseEntity.ok(response);
    }

    /**
     * 清空当前用户的所有搜索历史
     * DELETE /api/search-history
     */
    @Operation(summary = "清空搜索历史", description = "清空当前用户的所有搜索历史记录")
    @DeleteMapping
    public ResponseEntity<Map<String, String>> clearSearchHistory(HttpServletRequest request) {
        Long userId = getUserIdFromRequest(request);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        searchHistoryService.clearUserSearchHistory(userId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "All search history cleared successfully");
        return ResponseEntity.ok(response);
    }

    // =================== 以下为原有接口，保持向后兼容 ===================

    /**
     * Add a search history record for a user.
     */
    @Operation(summary = "添加搜索历史", description = "为指定用户添加搜索历史记录")
    @PostMapping("/add")
    public String addHistory(
        @Parameter(description = "用户ID") @RequestParam Long userId,
        @Parameter(description = "搜索关键词") @RequestParam String keyword
    ) {
        searchHistoryService.addHistory(userId, keyword);
        return "success";
    }

    /**
     * Get the list of search history keywords for a user.
     */
    @Operation(summary = "获取搜索关键词列表", description = "获取指定用户的搜索关键词列表")
    @GetMapping("/list")
    public List<String> listHistory(
        @Parameter(description = "用户ID") @RequestParam Long userId
    ) {
        return searchHistoryService.listHistory(userId);
    }

    /**
     * Batch remove search history keywords for a user.
     */
    @Operation(summary = "批量删除搜索历史", description = "批量删除指定用户的搜索历史记录")
    @PostMapping("/batchRemove")
    public String batchRemove(
        @Parameter(description = "用户ID") @RequestParam Long userId,
        @RequestBody List<String> keywords
    ) {
        searchHistoryService.batchRemove(userId, keywords);
        return "success";
    }

    /**
     * Clear all search history for a user.
     */
    @Operation(summary = "清空用户搜索历史", description = "清空指定用户的所有搜索历史记录")
    @DeleteMapping("/clear")
    public String clearHistory(
        @Parameter(description = "用户ID") @RequestParam Long userId
    ) {
        searchHistoryService.clearHistory(userId);
        return "success";
    }

    // =================== 私有方法 ===================

    /**
     * 从请求中提取用户ID（这里暂时返回null，实际应该从JWT中解析）
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        // TODO: 实现JWT token解析
        // 在实际应用中，这里应该从JWT token中解析用户ID
        return null;
    }
}
