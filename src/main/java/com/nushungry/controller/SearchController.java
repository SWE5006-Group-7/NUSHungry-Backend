package com.nushungry.controller;

import com.nushungry.dto.search.SearchRequest;
import com.nushungry.dto.search.SearchResult;
import com.nushungry.dto.search.SearchSuggestion;
import com.nushungry.model.Cafeteria;
import com.nushungry.model.Stall;
import com.nushungry.model.User;
import com.nushungry.service.SearchService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索控制器
 * 提供搜索、筛选、建议等API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/search")
public class SearchController {

    @Autowired
    private SearchService searchService;

    /**
     * 综合搜索(摊位+食堂)
     * GET /api/search?keyword=xxx&searchType=ALL&page=0&size=20
     */
    @GetMapping
    public ResponseEntity<SearchResult<Object>> search(
            @ModelAttribute SearchRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId();
        String ipAddress = getClientIp(httpRequest);

        log.info("搜索请求: keyword={}, type={}, user={}",
                request.getKeyword(), request.getSearchType(), userId);

        SearchResult<Object> result = searchService.search(request, userId, ipAddress);
        return ResponseEntity.ok(result);
    }

    /**
     * 搜索摊位
     * POST /api/search/stalls
     */
    @PostMapping("/stalls")
    public ResponseEntity<SearchResult<Stall>> searchStalls(
            @RequestBody SearchRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getCurrentUserId();
        String ipAddress = getClientIp(httpRequest);

        log.info("摊位搜索: keyword={}, filters={}", request.getKeyword(), request);

        SearchResult<Stall> result = searchService.searchStalls(request);

        // 记录搜索历史
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            // 历史记录在综合搜索中统一处理
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 搜索食堂
     * POST /api/search/cafeterias
     */
    @PostMapping("/cafeterias")
    public ResponseEntity<SearchResult<Cafeteria>> searchCafeterias(
            @RequestBody SearchRequest request
    ) {
        log.info("食堂搜索: keyword={}", request.getKeyword());

        SearchResult<Cafeteria> result = searchService.searchCafeterias(request);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取搜索建议
     * GET /api/search/suggestions?q=xxx
     */
    @GetMapping("/suggestions")
    public ResponseEntity<SearchSuggestion> getSearchSuggestions(
            @RequestParam(required = false) String q
    ) {
        Long userId = getCurrentUserId();

        log.info("搜索建议: prefix={}, user={}", q, userId);

        SearchSuggestion suggestions = searchService.getSearchSuggestions(q, userId);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * 获取筛选选项(菜系类型、食堂列表等)
     * GET /api/search/filter-options
     */
    @GetMapping("/filter-options")
    public ResponseEntity<Map<String, Object>> getFilterOptions() {
        Map<String, Object> options = new HashMap<>();

        // 获取所有菜系类型
        List<String> cuisineTypes = searchService.getAllCuisineTypes();
        options.put("cuisineTypes", cuisineTypes);

        // 获取所有食堂
        List<Object[]> cafeterias = searchService.getAllCafeterias();
        options.put("cafeterias", cafeterias);

        // 评分范围
        options.put("ratingRange", Map.of("min", 0.0, "max", 5.0));

        return ResponseEntity.ok(options);
    }

    /**
     * 获取推荐摊位
     * GET /api/search/recommended?limit=10
     */
    @GetMapping("/recommended")
    public ResponseEntity<List<Stall>> getRecommendedStalls(
            @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("获取推荐摊位: limit={}", limit);

        List<Stall> stalls = searchService.getRecommendedStalls(limit);
        return ResponseEntity.ok(stalls);
    }

    /**
     * 获取热门搜索关键词
     * GET /api/search/popular-keywords?limit=10
     */
    @GetMapping("/popular-keywords")
    public ResponseEntity<List<String>> getPopularKeywords(
            @RequestParam(defaultValue = "10") int limit
    ) {
        Long userId = getCurrentUserId();
        SearchSuggestion suggestions = searchService.getSearchSuggestions("", userId);
        return ResponseEntity.ok(suggestions.getPopularKeywords());
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getPrincipal())) {
                User user = (User) authentication.getPrincipal();
                return user.getId();
            }
        } catch (Exception e) {
            log.debug("获取用户ID失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 如果是多级代理,取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
