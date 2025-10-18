package com.nushungry.preference.repository;

import com.nushungry.preference.entity.SearchHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface SearchHistoryRepository extends JpaRepository<SearchHistory, Long> {
    List<SearchHistory> findByUserId(Long userId);
    void deleteByUserIdAndKeyword(Long userId, String keyword);
    @Modifying
    @Transactional
    @Query("DELETE FROM SearchHistory s WHERE s.userId = :userId AND s.keyword IN :keywords")
    void deleteByUserIdAndKeywordIn(@Param("userId") Long userId, @Param("keywords") List<String> keywords);
    @Modifying
    @Transactional
    @Query("DELETE FROM SearchHistory s WHERE s.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
