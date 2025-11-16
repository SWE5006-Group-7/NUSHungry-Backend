package com.nushungry.controller;

import com.nushungry.model.SearchHistory;
import com.nushungry.service.SearchHistoryService;
import com.nushungry.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索历史控制器
 */
@RestController
@RequestMapping("/api/search-history")
public class SearchHistoryController {

    @Autowired
    private SearchHistoryService searchHistoryService;

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 获取当前用户的搜索历史（分页）
     * GET /api/search-history?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getUserSearchHistory(
        HttpServletRequest request,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
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
    @GetMapping("/recent")
    public ResponseEntity<List<SearchHistory>> getRecentSearches(
        HttpServletRequest request,
        @RequestParam(defaultValue = "10") int limit
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
    @GetMapping("/keywords")
    public ResponseEntity<List<String>> getRecentKeywords(
        HttpServletRequest request,
        @RequestParam(defaultValue = "10") int limit
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
    @GetMapping("/popular")
    public ResponseEntity<List<Map<String, Object>>> getPopularKeywords(
        @RequestParam(defaultValue = "7") int days,
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<Map<String, Object>> keywords = searchHistoryService.getPopularKeywords(days, limit);
        return ResponseEntity.ok(keywords);
    }

    /**
     * 删除当前用户的单条搜索历史
     * DELETE /api/search-history/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteSearchHistory(
        HttpServletRequest request,
        @PathVariable Long id
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

    /**
     * 从请求中提取用户ID
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        String token = extractToken(request);
        if (token != null && jwtUtil.validateToken(token)) {
            return jwtUtil.extractUserId(token);
        }
        return null;
    }

    /**
     * 从请求头中提取JWT token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
