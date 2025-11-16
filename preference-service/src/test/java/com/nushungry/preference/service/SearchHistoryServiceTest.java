package com.nushungry.preference.service;

import com.nushungry.preference.entity.SearchHistory;
import com.nushungry.preference.repository.SearchHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SearchHistoryServiceTest {
    @Mock
    private SearchHistoryRepository searchHistoryRepository;

    @InjectMocks
    private SearchHistoryService searchHistoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testAddHistory() {
        searchHistoryService.addHistory(1L, "noodle");
        verify(searchHistoryRepository, times(1)).save(any(SearchHistory.class));
    }

    @Test
    void testListHistory() {
        List<SearchHistory> histories = new ArrayList<>();
        SearchHistory h = new SearchHistory();
        h.setKeyword("noodle");
        histories.add(h);
        when(searchHistoryRepository.findByUserId(1L)).thenReturn(histories);
        List<String> result = searchHistoryService.listHistory(1L);
        assertEquals(1, result.size());
        assertEquals("noodle", result.get(0));
    }

    @Test
    void testBatchRemove() {
        List<String> keywords = List.of("noodle", "rice");
        searchHistoryService.batchRemove(1L, keywords);
        verify(searchHistoryRepository, times(1)).deleteByUserIdAndKeywordIn(1L, keywords);
    }

    @Test
    void testClearHistory() {
        searchHistoryService.clearHistory(1L);
        verify(searchHistoryRepository, times(1)).deleteByUserId(1L);
    }

    @Test
    void testRecordSearch_EmptyKeyword_ShouldNotSave() {
        // 测试空搜索词过滤：空关键词不应保存
        searchHistoryService.recordSearch(1L, "", "STALL", 0, null);
        verify(searchHistoryRepository, never()).save(any(SearchHistory.class));
    }

    @Test
    void testRecordSearch_NullKeyword_ShouldNotSave() {
        // 测试null搜索词过滤
        searchHistoryService.recordSearch(1L, null, "STALL", 0, null);
        verify(searchHistoryRepository, never()).save(any(SearchHistory.class));
    }

    @Test
    void testRecordSearch_BlankKeyword_ShouldNotSave() {
        // 测试空白字符搜索词过滤
        searchHistoryService.recordSearch(1L, "   ", "STALL", 0, null);
        verify(searchHistoryRepository, never()).save(any(SearchHistory.class));
    }

    @Test
    void testGetUserRecentSearches_LimitTooLarge_ShouldDefault() {
        // 测试搜索历史上限：limit > 50 时应重置为10
        List<SearchHistory> mockResult = new ArrayList<>();
        when(searchHistoryRepository.findTop10ByUserIdOrderBySearchTimeDesc(1L)).thenReturn(mockResult);

        List<SearchHistory> result = searchHistoryService.getUserRecentSearches(1L, 100);
        // 验证调用了正确的repository方法
        verify(searchHistoryRepository, times(1)).findTop10ByUserIdOrderBySearchTimeDesc(1L);
        assertNotNull(result);
    }

    @Test
    void testGetUserRecentSearches_LimitNegative_ShouldDefault() {
        // 测试搜索历史上限：limit <= 0 时应重置为10
        List<SearchHistory> mockResult = new ArrayList<>();
        when(searchHistoryRepository.findTop10ByUserIdOrderBySearchTimeDesc(1L)).thenReturn(mockResult);

        List<SearchHistory> result = searchHistoryService.getUserRecentSearches(1L, -5);
        verify(searchHistoryRepository, times(1)).findTop10ByUserIdOrderBySearchTimeDesc(1L);
        assertNotNull(result);
    }

    @Test
    void testGetUserRecentKeywords_LimitBoundary() {
        // 测试关键词上限：limit > 50 时应重置为10
        List<String> mockKeywords = new ArrayList<>();
        when(searchHistoryRepository.findDistinctKeywordsByUserId(1L, 10)).thenReturn(mockKeywords);

        List<String> result = searchHistoryService.getUserRecentKeywords(1L, 100);
        verify(searchHistoryRepository, times(1)).findDistinctKeywordsByUserId(1L, 10);
        assertNotNull(result);
    }

    @Test
    void testListHistory_Empty() {
        when(searchHistoryRepository.findByUserId(1L)).thenReturn(new ArrayList<>());
        List<String> result = searchHistoryService.listHistory(1L);
        assertTrue(result.isEmpty());
    }

    @Test
    void testBatchRemove_EmptyList() {
        List<String> emptyKeywords = new ArrayList<>();
        searchHistoryService.batchRemove(1L, emptyKeywords);
        verify(searchHistoryRepository, times(1)).deleteByUserIdAndKeywordIn(1L, emptyKeywords);
    }

    @Test
    void testDeleteSearchHistory_WrongUser_ShouldNotDelete() {
        // 测试删除时用户ID不匹配：应该不删除
        SearchHistory history = new SearchHistory();
        history.setId(1L);
        history.setUserId(2L); // 不同用户
        history.setKeyword("test");

        when(searchHistoryRepository.findById(1L)).thenReturn(java.util.Optional.of(history));

        searchHistoryService.deleteSearchHistory(1L, 1L); // userId=1 尝试删除
        verify(searchHistoryRepository, never()).delete(any(SearchHistory.class));
    }

    @Test
    void testDeleteSearchHistory_NotFound_ShouldNotThrow() {
        // 测试删除不存在的记录：应正常处理
        when(searchHistoryRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertDoesNotThrow(() -> searchHistoryService.deleteSearchHistory(999L, 1L));
        verify(searchHistoryRepository, never()).delete(any(SearchHistory.class));
    }
}

