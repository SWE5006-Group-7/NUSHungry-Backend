package com.nushungry.preference.service;

import com.nushungry.preference.entity.SearchHistory;
import com.nushungry.preference.repository.SearchHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class SearchHistoryService {
    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    /**
     * Add a search history record for a user.
     */
    public void addHistory(Long userId, String keyword) {
        SearchHistory history = new SearchHistory();
        history.setUserId(userId);
        history.setKeyword(keyword);
        history.setCreatedAt(System.currentTimeMillis());
        searchHistoryRepository.save(history);
    }

    /**
     * List all search history keywords for a user.
     */
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
     */
    public void batchRemove(Long userId, List<String> keywords) {
        searchHistoryRepository.deleteByUserIdAndKeywordIn(userId, keywords);
    }

    /**
     * Clear all search history for a user.
     */
    public void clearHistory(Long userId) {
        searchHistoryRepository.deleteByUserId(userId);
    }
}

