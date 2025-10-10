package com.nushungry.service;

import com.nushungry.dto.search.SearchRequest;
import com.nushungry.dto.search.SearchResult;
import com.nushungry.dto.search.SearchSuggestion;
import com.nushungry.model.Cafeteria;
import com.nushungry.model.SearchHistory;
import com.nushungry.model.Stall;
import com.nushungry.repository.CafeteriaRepository;
import com.nushungry.repository.SearchHistoryRepository;
import com.nushungry.repository.StallRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 搜索服务
 * 提供统一的搜索接口,支持多条件筛选、智能排序、搜索建议等功能
 */
@Slf4j
@Service
public class SearchService {

    @Autowired
    private StallRepository stallRepository;

    @Autowired
    private CafeteriaRepository cafeteriaRepository;

    @Autowired
    private SearchHistoryRepository searchHistoryRepository;

    /**
     * 综合搜索(摊位+食堂)
     */
    @Transactional
    public SearchResult<Object> search(SearchRequest request, Long userId, String ipAddress) {
        long startTime = System.currentTimeMillis();

        SearchResult<Object> result = new SearchResult<>();
        result.setKeyword(request.getKeyword());
        result.setCurrentPage(request.getPage());
        result.setPageSize(request.getSize());

        List<Object> allResults = new ArrayList<>();
        long totalCount = 0;

        // 根据搜索类型执行搜索
        if (request.getSearchType() == SearchHistory.SearchType.ALL ||
            request.getSearchType() == SearchHistory.SearchType.STALL) {
            SearchResult<Stall> stallResult = searchStalls(request);
            allResults.addAll(stallResult.getResults());
            totalCount += stallResult.getTotalCount();
        }

        if (request.getSearchType() == SearchHistory.SearchType.ALL ||
            request.getSearchType() == SearchHistory.SearchType.CAFETERIA) {
            SearchResult<Cafeteria> cafeteriaResult = searchCafeterias(request);
            allResults.addAll(cafeteriaResult.getResults());
            totalCount += cafeteriaResult.getTotalCount();
        }

        result.setResults(allResults);
        result.setTotalCount(totalCount);
        result.setTotalPages((int) Math.ceil((double) totalCount / request.getSize()));
        result.setSearchTime(System.currentTimeMillis() - startTime);

        // 记录搜索历史
        if (request.getKeyword() != null && !request.getKeyword().trim().isEmpty()) {
            saveSearchHistory(request, userId, ipAddress, (int) totalCount);
        }

        return result;
    }

    /**
     * 搜索摊位
     */
    public SearchResult<Stall> searchStalls(SearchRequest request) {
        Pageable pageable = createPageable(request);

        Page<Stall> page;

        // 如果没有关键词,返回所有结果(带筛选)
        if (request.getKeyword() == null || request.getKeyword().trim().isEmpty()) {
            page = stallRepository.searchWithFilters(
                    null,
                    request.getCuisineTypes(),
                    request.getMinRating(),
                    request.getMaxRating(),
                    request.getCafeteriaId(),
                    request.getHalalOnly(),
                    pageable
            );
        } else {
            // 使用完整搜索
            page = stallRepository.searchWithFilters(
                    request.getKeyword(),
                    request.getCuisineTypes(),
                    request.getMinRating(),
                    request.getMaxRating(),
                    request.getCafeteriaId(),
                    request.getHalalOnly(),
                    pageable
            );
        }

        return convertToSearchResult(page, request);
    }

    /**
     * 搜索食堂
     */
    public SearchResult<Cafeteria> searchCafeterias(SearchRequest request) {
        Pageable pageable = PageRequest.of(
                request.getPage(),
                request.getSize(),
                Sort.by(Sort.Direction.DESC, "name")
        );

        Page<Cafeteria> page;

        if (request.getKeyword() == null || request.getKeyword().trim().isEmpty()) {
            page = cafeteriaRepository.findAll(pageable);
        } else {
            page = cafeteriaRepository.searchByKeyword(request.getKeyword(), pageable);
        }

        return convertToSearchResult(page, request);
    }

    /**
     * 获取搜索建议
     */
    public SearchSuggestion getSearchSuggestions(String prefix, Long userId) {
        SearchSuggestion suggestion = new SearchSuggestion();

        // 获取搜索历史建议
        if (prefix != null && !prefix.trim().isEmpty()) {
            List<String> historySuggestions = searchHistoryRepository.findSuggestionsByPrefix(
                    prefix, PageRequest.of(0, 10)
            );

            // 获取摊位名称建议
            List<String> stallNames = stallRepository.findNamesByPrefix(
                    prefix, PageRequest.of(0, 5)
            );

            // 获取食堂名称建议
            List<String> cafeteriaNames = cafeteriaRepository.findNamesByPrefix(
                    prefix, PageRequest.of(0, 5)
            );

            // 合并建议
            List<SearchSuggestion.Suggestion> allSuggestions = new ArrayList<>();

            // 添加历史建议
            historySuggestions.forEach(text ->
                    allSuggestions.add(new SearchSuggestion.Suggestion(
                            text, SearchSuggestion.SuggestionType.KEYWORD, null, 1.0
                    ))
            );

            // 添加摊位名称建议
            stallNames.forEach(text ->
                    allSuggestions.add(new SearchSuggestion.Suggestion(
                            text, SearchSuggestion.SuggestionType.STALL_NAME, null, 0.9
                    ))
            );

            // 添加食堂名称建议
            cafeteriaNames.forEach(text ->
                    allSuggestions.add(new SearchSuggestion.Suggestion(
                            text, SearchSuggestion.SuggestionType.CAFETERIA_NAME, null, 0.8
                    ))
            );

            suggestion.setSuggestions(allSuggestions.stream()
                    .limit(10)
                    .collect(Collectors.toList()));
        }

        // 获取热门搜索
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<Object[]> popularData = searchHistoryRepository.findPopularKeywords(
                oneWeekAgo, PageRequest.of(0, 10)
        );
        List<String> popularKeywords = popularData.stream()
                .map(arr -> (String) arr[0])
                .collect(Collectors.toList());
        suggestion.setPopularKeywords(popularKeywords);

        // 获取用户最近搜索
        if (userId != null) {
            List<String> recentSearches = searchHistoryRepository.findRecentKeywordsByUserId(
                    userId, PageRequest.of(0, 10)
            );
            suggestion.setRecentSearches(recentSearches);
        }

        return suggestion;
    }

    /**
     * 获取所有菜系类型
     */
    public List<String> getAllCuisineTypes() {
        return stallRepository.findAllCuisineTypes();
    }

    /**
     * 获取所有食堂(ID和名称)
     */
    public List<Object[]> getAllCafeterias() {
        return cafeteriaRepository.findAllIdAndName();
    }

    /**
     * 获取推荐摊位
     */
    public List<Stall> getRecommendedStalls(int limit) {
        return stallRepository.findRecommendedStalls(5, PageRequest.of(0, limit));
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 创建Pageable对象(包含排序)
     */
    private Pageable createPageable(SearchRequest request) {
        Sort sort = createSort(request);
        return PageRequest.of(request.getPage(), request.getSize(), sort);
    }

    /**
     * 创建排序对象
     */
    private Sort createSort(SearchRequest request) {
        Sort.Direction direction = request.getSortDirection() == SearchRequest.SortDirection.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return switch (request.getSortBy()) {
            case RATING -> Sort.by(direction, "averageRating");
            case REVIEW_COUNT -> Sort.by(direction, "reviewCount");
            case NAME -> Sort.by(direction, "name");
            case RELEVANCE -> Sort.by(direction, "averageRating", "reviewCount");
            default -> Sort.by(Sort.Direction.DESC, "averageRating");
        };
    }

    /**
     * 将Page对象转换为SearchResult
     */
    private <T> SearchResult<T> convertToSearchResult(Page<T> page, SearchRequest request) {
        SearchResult<T> result = new SearchResult<>();
        result.setResults(page.getContent());
        result.setTotalCount(page.getTotalElements());
        result.setCurrentPage(page.getNumber());
        result.setPageSize(page.getSize());
        result.setTotalPages(page.getTotalPages());
        result.setKeyword(request.getKeyword());
        return result;
    }

    /**
     * 保存搜索历史
     */
    private void saveSearchHistory(SearchRequest request, Long userId, String ipAddress, int resultCount) {
        try {
            SearchHistory history = new SearchHistory();
            history.setUserId(userId);
            history.setKeyword(request.getKeyword());
            history.setSearchType(request.getSearchType());
            history.setResultCount(resultCount);
            history.setIpAddress(ipAddress);
            searchHistoryRepository.save(history);
        } catch (Exception e) {
            log.error("保存搜索历史失败: {}", e.getMessage());
            // 不影响主流程
        }
    }
}
