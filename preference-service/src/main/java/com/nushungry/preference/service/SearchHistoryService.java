package com.nushungry.preference.service;

import com.nushungry.preference.entity.SearchHistory;
import com.nushungry.preference.repository.SearchHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 搜索历史服务层
 *
 * 缓存策略：
 * - listHistory(): 缓存用户搜索历史列表（searchHistory::{userId}）
 * - getUserRecentSearches(): 缓存用户最近搜索历史（searchHistory::{userId}::recent）
 * - getUserRecentKeywords(): 缓存用户最近关键词（searchHistory::{userId}::keywords）
 * - addHistory(): 清除用户搜索历史缓存
 * - batchRemove(): 清除用户搜索历史缓存
 * - clearHistory(): 清除用户搜索历史缓存
 */
@Service
@Transactional
public class SearchHistoryService {
    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    /**
     * 异步记录搜索历史
     */
    @Async
    public void recordSearch(Long userId, String keyword, String searchType,
                            Integer resultCount, HttpServletRequest request) {
        try {
            System.out.println("====== 开始记录搜索历史 ======");
            System.out.println("userId: " + userId);
            System.out.println("keyword: " + keyword);
            System.out.println("searchType: " + searchType);
            System.out.println("resultCount: " + resultCount);

            if (!StringUtils.hasText(keyword)) {
                System.out.println("关键词为空，不记录");
                return;
            }

            SearchHistory history = new SearchHistory();
            history.setUserId(userId);
            history.setKeyword(keyword.trim());
            history.setSearchType(searchType);
            history.setResultCount(resultCount);

            // 获取用户IP
            if (request != null) {
                history.setIpAddress(getClientIpAddress(request));
                System.out.println("ipAddress: " + history.getIpAddress());
            }

            SearchHistory saved = searchHistoryRepository.save(history);
            System.out.println("搜索历史保存成功，ID: " + saved.getId());
            System.out.println("====== 记录搜索历史完成 ======");
        } catch (Exception e) {
            System.err.println("====== 记录搜索历史失败 ======");
            System.err.println("错误信息: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Add a search history record for a user.
     * 清除该用户的搜索历史缓存
     */
    @CacheEvict(value = "searchHistory", allEntries = true)
    public void addHistory(Long userId, String keyword) {
        SearchHistory history = new SearchHistory();
        history.setUserId(userId);
        history.setKeyword(keyword);
        // 自动设置searchTime通过@PrePersist
        searchHistoryRepository.save(history);
    }

    /**
     * List all search history keywords for a user.
     * 缓存用户搜索历史列表，TTL 5分钟
     */
    @Cacheable(value = "searchHistory", key = "#userId", unless = "#result == null || #result.isEmpty()")
    public List<String> listHistory(Long userId) {
        List<SearchHistory> histories = searchHistoryRepository.findByUserId(userId);
        List<String> result = new ArrayList<>();
        for (SearchHistory h : histories) {
            result.add(h.getKeyword());
        }
        return result;
    }

    /**
     * 获取用户搜索历史（分页）
     */
    public Page<SearchHistory> getUserSearchHistory(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return searchHistoryRepository.findByUserIdOrderBySearchTimeDesc(userId, pageable);
    }

    /**
     * 获取用户最近的搜索历史（限制数量）
     */
    @Cacheable(value = "searchHistory", key = "'recent:' + #userId + ':' + #limit", unless = "#result == null || #result.isEmpty()")
    public List<SearchHistory> getUserRecentSearches(Long userId, int limit) {
        if (limit <= 0 || limit > 50) {
            limit = 10;
        }
        return searchHistoryRepository.findTop10ByUserIdOrderBySearchTimeDesc(userId);
    }

    /**
     * 获取用户最近的唯一搜索关键词（去重）
     */
    @Cacheable(value = "searchHistory", key = "'keywords:' + #userId + ':' + #limit", unless = "#result == null || #result.isEmpty()")
    public List<String> getUserRecentKeywords(Long userId, int limit) {
        if (limit <= 0 || limit > 50) {
            limit = 10;
        }
        System.out.println(">>> getUserRecentKeywords 被调用");
        System.out.println(">>> userId: " + userId);
        System.out.println(">>> limit: " + limit);

        List<String> keywords = searchHistoryRepository.findDistinctKeywordsByUserId(userId, limit);

        System.out.println(">>> 查询结果: " + keywords);
        System.out.println(">>> 结果数量: " + (keywords != null ? keywords.size() : 0));

        return keywords;
    }

    /**
     * 获取热门搜索关键词
     */
    public List<Map<String, Object>> getPopularKeywords(int days, int limit) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Pageable pageable = PageRequest.of(0, limit);

        List<Object[]> results = searchHistoryRepository.findPopularKeywords(since, pageable);

        return results.stream()
            .map(result -> {
                Map<String, Object> map = new HashMap<>();
                map.put("keyword", result[0]);
                map.put("count", result[1]);
                return map;
            })
            .collect(Collectors.toList());
    }

    /**
     * Batch remove search history keywords for a user.
     * 清除该用户的搜索历史缓存
     */
    @Transactional
    @CacheEvict(value = "searchHistory", allEntries = true)
    public void batchRemove(Long userId, List<String> keywords) {
        searchHistoryRepository.deleteByUserIdAndKeywordIn(userId, keywords);
    }

    /**
     * Clear all search history for a user.
     * 清除该用户的搜索历史缓存
     */
    @Transactional
    @CacheEvict(value = "searchHistory", allEntries = true)
    public void clearHistory(Long userId) {
        searchHistoryRepository.deleteByUserId(userId);
    }

    /**
     * 删除用户的所有搜索历史
     */
    @Transactional
    @CacheEvict(value = "searchHistory", allEntries = true)
    public void clearUserSearchHistory(Long userId) {
        searchHistoryRepository.deleteByUserId(userId);
    }

    /**
     * 删除用户的单条搜索历史
     */
    @Transactional
    @CacheEvict(value = "searchHistory", allEntries = true)
    public void deleteSearchHistory(Long id, Long userId) {
        SearchHistory history = searchHistoryRepository.findById(id).orElse(null);
        if (history != null && history.getUserId().equals(userId)) {
            searchHistoryRepository.delete(history);
        }
    }

    /**
     * 清理旧的搜索历史（定时任务）
     */
    @Transactional
    public void cleanOldSearchHistory(int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        searchHistoryRepository.deleteBySearchTimeBefore(cutoffDate);
    }

    /**
     * 统计用户的搜索次数
     */
    public long getUserSearchCount(Long userId) {
        return searchHistoryRepository.countByUserId(userId);
    }

    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_FORWARDED");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_VIA");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}

