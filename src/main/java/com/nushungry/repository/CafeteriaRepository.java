package com.nushungry.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import com.nushungry.model.Cafeteria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CafeteriaRepository extends JpaRepository<Cafeteria, Long> {

    @Query("SELECT c FROM Cafeteria c JOIN c.stalls s JOIN s.reviews r GROUP BY c.id ORDER BY AVG(r.rating) DESC")
    List<Cafeteria> findPopularCafeterias();

    @Query("SELECT c FROM Cafeteria c LEFT JOIN FETCH c.stalls WHERE c.id = :id")
    Optional<Cafeteria> findByIdWithStalls(@Param("id") Long id);

    // ==================== 搜索相关方法 ====================

    /**
     * 按关键词搜索食堂(支持名称和位置模糊匹配)
     */
    @Query("SELECT c FROM Cafeteria c WHERE " +
           "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.location) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Cafeteria> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 按名称前缀搜索(用于自动补全)
     */
    @Query("SELECT c.name FROM Cafeteria c WHERE LOWER(c.name) LIKE LOWER(CONCAT(:prefix, '%'))")
    List<String> findNamesByPrefix(@Param("prefix") String prefix, Pageable pageable);

    /**
     * 获取所有食堂名称(用于筛选面板)
     */
    @Query("SELECT c.id, c.name FROM Cafeteria c ORDER BY c.name")
    List<Object[]> findAllIdAndName();
}