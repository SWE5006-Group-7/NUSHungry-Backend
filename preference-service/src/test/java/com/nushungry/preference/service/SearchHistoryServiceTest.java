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
}

