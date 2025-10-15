package com.nushungry.repository;

import com.nushungry.model.SearchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {

    /**
     * 查找用户的搜索历史（分页）
     */
    Page<SearchHistory> findByUserIdOrderBySearchTimeDesc(Long userId, Pageable pageable);

    /**
     * 查找用户的搜索历史（限制数量）
     */
    List<SearchHistory> findTop10ByUserIdOrderBySearchTimeDesc(Long userId);

    /**
     * 查找用户最近的唯一搜索关键词（去重）
     * 使用GROUP BY和MAX(search_time)实现去重和排序，兼容MySQL ONLY_FULL_GROUP_BY模式
     */
    @Query(value = "SELECT keyword FROM search_history WHERE user_id = :userId GROUP BY keyword ORDER BY MAX(search_time) DESC LIMIT :limit", nativeQuery = true)
    List<String> findDistinctKeywordsByUserId(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查找热门搜索关键词（所有用户）
     */
    @Query("SELECT sh.keyword, COUNT(sh.keyword) as count FROM SearchHistory sh " +
           "WHERE sh.searchTime > :since " +
           "GROUP BY sh.keyword " +
           "ORDER BY count DESC")
    List<Object[]> findPopularKeywords(@Param("since") LocalDateTime since, Pageable pageable);

    /**
     * 删除用户的所有搜索历史
     */
    void deleteByUserId(Long userId);

    /**
     * 删除指定时间之前的搜索历史（清理旧数据）
     */
    void deleteBySearchTimeBefore(LocalDateTime dateTime);

    /**
     * 统计用户的搜索次数
     */
    long countByUserId(Long userId);
}
