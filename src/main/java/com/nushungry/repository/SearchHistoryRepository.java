package com.nushungry.repository;

import com.nushungry.model.SearchHistory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 搜索历史数据访问层
 */
@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    /**
     * 获取用户的搜索历史(分页)
     */
    List<SearchHistory> findByUserIdOrderBySearchedAtDesc(Long userId, Pageable pageable);

    /**
     * 获取用户的搜索历史(按类型筛选)
     */
    List<SearchHistory> findByUserIdAndSearchTypeOrderBySearchedAtDesc(
            Long userId,
            SearchHistory.SearchType searchType,
            Pageable pageable
    );

    /**
     * 获取热门搜索关键词(所有用户)
     * 按搜索频率排序
     */
    @Query("SELECT s.keyword, COUNT(s) as count FROM SearchHistory s " +
           "WHERE s.searchedAt >= :since " +
           "GROUP BY s.keyword " +
           "ORDER BY count DESC")
    List<Object[]> findPopularKeywords(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * 获取用户的最近搜索关键词(去重)
     */
    @Query("SELECT DISTINCT s.keyword FROM SearchHistory s " +
           "WHERE s.userId = :userId " +
           "ORDER BY s.searchedAt DESC")
    List<String> findRecentKeywordsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * 获取搜索建议(基于关键词前缀匹配)
     */
    @Query("SELECT DISTINCT s.keyword FROM SearchHistory s " +
           "WHERE s.keyword LIKE CONCAT(:prefix, '%') " +
           "AND s.resultCount > 0 " +
           "ORDER BY s.searchedAt DESC")
    List<String> findSuggestionsByPrefix(@Param("prefix") String prefix, Pageable pageable);

    /**
     * 删除用户指定时间之前的搜索历史
     */
    void deleteByUserIdAndSearchedAtBefore(Long userId, LocalDateTime before);

    /**
     * 删除所有用户指定时间之前的搜索历史(用于定期清理)
     */
    void deleteBySearchedAtBefore(LocalDateTime before);

    /**
     * 统计用户的搜索次数
     */
    long countByUserId(Long userId);

    /**
     * 统计指定时间段内的搜索次数
     */
    long countBySearchedAtBetween(LocalDateTime start, LocalDateTime end);
}
