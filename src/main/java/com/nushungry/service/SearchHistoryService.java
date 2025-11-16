package com.nushungry.service;

import com.nushungry.model.SearchHistory;
import com.nushungry.repository.SearchHistoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
     * 获取用户搜索历史（分页）
     */
    public Page<SearchHistory> getUserSearchHistory(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return searchHistoryRepository.findByUserIdOrderBySearchTimeDesc(userId, pageable);
    }

    /**
     * 获取用户最近的搜索历史（限制数量）
     */
    public List<SearchHistory> getUserRecentSearches(Long userId, int limit) {
        if (limit <= 0 || limit > 50) {
            limit = 10;
        }
        Pageable pageable = PageRequest.of(0, limit);
        return searchHistoryRepository.findTop10ByUserIdOrderBySearchTimeDesc(userId);
    }

    /**
     * 获取用户最近的唯一搜索关键词（去重）
     */
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
     * 删除用户的所有搜索历史
     */
    public void clearUserSearchHistory(Long userId) {
        searchHistoryRepository.deleteByUserId(userId);
    }

    /**
     * 删除用户的单条搜索历史
     */
    public void deleteSearchHistory(Long id, Long userId) {
        SearchHistory history = searchHistoryRepository.findById(id).orElse(null);
        if (history != null && history.getUserId().equals(userId)) {
            searchHistoryRepository.delete(history);
        }
    }

    /**
     * 清理旧的搜索历史（定时任务）
     */
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
