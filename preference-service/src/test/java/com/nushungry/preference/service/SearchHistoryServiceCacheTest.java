package com.nushungry.preference.service;

import com.nushungry.preference.entity.SearchHistory;
import com.nushungry.preference.repository.SearchHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * SearchHistoryService 缓存功能测试
 * 
 * 测试 Redis 缓存是否正常工作
 * 测试缓存的存储和清除逻辑
 */
@SpringBootTest
@ActiveProfiles("test")
class SearchHistoryServiceCacheTest {

    @Autowired
    private SearchHistoryService searchHistoryService;

    @MockBean
    private SearchHistoryRepository searchHistoryRepository;

    @Autowired(required = false)
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        // 清除所有缓存
        if (cacheManager != null) {
            cacheManager.getCacheNames().forEach(cacheName -> {
                var cache = cacheManager.getCache(cacheName);
                if (cache != null) {
                    cache.clear();
                }
            });
        }
        reset(searchHistoryRepository);
    }

    @Test
    void testListHistoryCaching() {
        // Given
        Long userId = 1L;
        SearchHistory history1 = new SearchHistory();
        history1.setUserId(userId);
        history1.setKeyword("chicken rice");
        history1.setSearchTime(LocalDateTime.now());

        SearchHistory history2 = new SearchHistory();
        history2.setUserId(userId);
        history2.setKeyword("laksa");
        history2.setSearchTime(LocalDateTime.now());

        List<SearchHistory> histories = Arrays.asList(history1, history2);
        when(searchHistoryRepository.findByUserId(userId)).thenReturn(histories);

        // When - 第一次调用
        List<String> result1 = searchHistoryService.listHistory(userId);
        
        // When - 第二次调用（应该从缓存获取）
        List<String> result2 = searchHistoryService.listHistory(userId);

        // Then
        assertThat(result1).hasSize(2).containsExactly("chicken rice", "laksa");
        assertThat(result2).hasSize(2).containsExactly("chicken rice", "laksa");
        
        // 验证 repository 只被调用一次（第二次从缓存获取）
        verify(searchHistoryRepository, times(1)).findByUserId(userId);
    }

    @Test
    void testAddHistoryClearsCaches() {
        // Given
        Long userId = 1L;
        String keyword = "nasi lemak";
        
        List<SearchHistory> histories = Arrays.asList(new SearchHistory());
        when(searchHistoryRepository.findByUserId(userId)).thenReturn(histories);

        // When - 填充缓存
        searchHistoryService.listHistory(userId);
        
        verify(searchHistoryRepository, times(1)).findByUserId(userId);

        // When - 添加搜索历史（应该清除缓存）
        searchHistoryService.addHistory(userId, keyword);

        // When - 再次查询（应该重新从数据库获取）
        searchHistoryService.listHistory(userId);

        // Then - 验证缓存被清除，重新从数据库查询
        verify(searchHistoryRepository, times(2)).findByUserId(userId);
    }

    @Test
    void testBatchRemoveClearsCaches() {
        // Given
        Long userId = 1L;
        List<String> keywords = Arrays.asList("chicken rice", "laksa");
        
        List<SearchHistory> histories = Arrays.asList(new SearchHistory());
        when(searchHistoryRepository.findByUserId(userId)).thenReturn(histories);

        // When - 填充缓存
        searchHistoryService.listHistory(userId);
        
        verify(searchHistoryRepository, times(1)).findByUserId(userId);

        // When - 批量删除（应该清除缓存）
        searchHistoryService.batchRemove(userId, keywords);

        // When - 再次查询
        searchHistoryService.listHistory(userId);

        // Then - 验证缓存被清除
        verify(searchHistoryRepository, times(2)).findByUserId(userId);
    }

    @Test
    void testClearHistoryClearsCaches() {
        // Given
        Long userId = 1L;
        
        List<SearchHistory> histories = Arrays.asList(new SearchHistory());
        when(searchHistoryRepository.findByUserId(userId)).thenReturn(histories);

        // When - 填充缓存
        searchHistoryService.listHistory(userId);
        
        verify(searchHistoryRepository, times(1)).findByUserId(userId);

        // When - 清空历史（应该清除缓存）
        searchHistoryService.clearHistory(userId);

        // When - 再次查询
        searchHistoryService.listHistory(userId);

        // Then - 验证缓存被清除
        verify(searchHistoryRepository, times(2)).findByUserId(userId);
    }

    @Test
    void testCacheManagerAvailable() {
        // 验证 CacheManager 是否可用（在测试环境中可能不可用）
        if (cacheManager != null) {
            assertThat(cacheManager.getCacheNames()).contains("searchHistory");
        }
    }
}
