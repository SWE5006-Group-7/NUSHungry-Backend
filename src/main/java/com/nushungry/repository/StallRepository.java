package com.nushungry.repository;

import com.nushungry.model.Stall;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface StallRepository extends JpaRepository<Stall, Long> {

    List<Stall> findByCafeteriaId(Long cafeteriaId);

    /**
     * 统计指定时间之前创建的摊位数量
     */
    long countByCreatedAtBefore(LocalDateTime dateTime);

    /**
     * 统计指定时间范围内创建的摊位数量
     */
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

    // ==================== 搜索相关方法 ====================

    /**
     * 按关键词搜索摊位(支持名称和菜系类型模糊匹配)
     */
    @Query("SELECT s FROM Stall s WHERE " +
           "LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.cuisineType) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Stall> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 按关键词和菜系类型搜索
     */
    @Query("SELECT s FROM Stall s WHERE " +
           "(LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.cuisineType) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "s.cuisineType IN :cuisineTypes")
    Page<Stall> searchByKeywordAndCuisineTypes(
            @Param("keyword") String keyword,
            @Param("cuisineTypes") List<String> cuisineTypes,
            Pageable pageable
    );

    /**
     * 按关键词、评分范围搜索
     */
    @Query("SELECT s FROM Stall s WHERE " +
           "(LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.cuisineType) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "s.averageRating >= :minRating AND s.averageRating <= :maxRating")
    Page<Stall> searchByKeywordAndRatingRange(
            @Param("keyword") String keyword,
            @Param("minRating") Double minRating,
            @Param("maxRating") Double maxRating,
            Pageable pageable
    );

    /**
     * 完整搜索(支持所有筛选条件)
     */
    @Query("SELECT s FROM Stall s WHERE " +
           "(:keyword IS NULL OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(s.cuisineType) LIKE LOWER(CONCAT('%', :keyword, '%'))) AND " +
           "(:cuisineTypes IS NULL OR s.cuisineType IN :cuisineTypes) AND " +
           "(:minRating IS NULL OR s.averageRating >= :minRating) AND " +
           "(:maxRating IS NULL OR s.averageRating <= :maxRating) AND " +
           "(:cafeteriaId IS NULL OR s.cafeteria.id = :cafeteriaId) AND " +
           "(:halalOnly IS NULL OR :halalOnly = false OR s.halalInfo IS NOT NULL)")
    Page<Stall> searchWithFilters(
            @Param("keyword") String keyword,
            @Param("cuisineTypes") List<String> cuisineTypes,
            @Param("minRating") Double minRating,
            @Param("maxRating") Double maxRating,
            @Param("cafeteriaId") Long cafeteriaId,
            @Param("halalOnly") Boolean halalOnly,
            Pageable pageable
    );

    /**
     * 获取所有不同的菜系类型(用于筛选面板)
     */
    @Query("SELECT DISTINCT s.cuisineType FROM Stall s WHERE s.cuisineType IS NOT NULL ORDER BY s.cuisineType")
    List<String> findAllCuisineTypes();

    /**
     * 按名称前缀搜索(用于自动补全)
     */
    @Query("SELECT s.name FROM Stall s WHERE LOWER(s.name) LIKE LOWER(CONCAT(:prefix, '%'))")
    List<String> findNamesByPrefix(@Param("prefix") String prefix, Pageable pageable);

    /**
     * 按评分和评价数量排序获取推荐摊位
     */
    @Query("SELECT s FROM Stall s WHERE s.reviewCount > :minReviewCount " +
           "ORDER BY (s.averageRating * 0.7 + (s.reviewCount / 100.0) * 0.3) DESC")
    List<Stall> findRecommendedStalls(@Param("minReviewCount") Integer minReviewCount, Pageable pageable);
}