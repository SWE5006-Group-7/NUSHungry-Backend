package com.nushungry.preference.service;

import com.nushungry.preference.entity.SearchHistory;
import com.nushungry.preference.repository.SearchHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索历史服务层
 * 
 * 缓存策略：
 * - listHistory(): 缓存用户搜索历史列表（searchHistory::{userId}）
 * - addHistory(): 清除用户搜索历史缓存
 * - batchRemove(): 清除用户搜索历史缓存
 * - clearHistory(): 清除用户搜索历史缓存
 */
@Service
public class SearchHistoryService {
    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    /**
     * Add a search history record for a user.
     * 清除该用户的搜索历史缓存
     */
    @CacheEvict(value = "searchHistory", key = "#userId")
    public void addHistory(Long userId, String keyword) {
        SearchHistory history = new SearchHistory();
        history.setUserId(userId);
        history.setKeyword(keyword);
        history.setCreatedAt(System.currentTimeMillis());
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
     * Batch remove search history keywords for a user.
     * 清除该用户的搜索历史缓存
     */
    @Transactional
    @CacheEvict(value = "searchHistory", key = "#userId")
    public void batchRemove(Long userId, List<String> keywords) {
        searchHistoryRepository.deleteByUserIdAndKeywordIn(userId, keywords);
    }

    /**
     * Clear all search history for a user.
     * 清除该用户的搜索历史缓存
     */
    @Transactional
    @CacheEvict(value = "searchHistory", key = "#userId")
    public void clearHistory(Long userId) {
        searchHistoryRepository.deleteByUserId(userId);
    }
}

