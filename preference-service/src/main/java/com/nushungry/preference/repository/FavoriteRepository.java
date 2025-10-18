package com.nushungry.preference.repository;

import com.nushungry.preference.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserId(Long userId);
    void deleteByUserIdAndStallId(Long userId, Long stallId);
    @Modifying
    @Transactional
    @Query("DELETE FROM Favorite f WHERE f.userId = :userId AND f.stallId IN :stallIds")
    void deleteByUserIdAndStallIdIn(@Param("userId") Long userId, @Param("stallIds") List<Long> stallIds);
    boolean existsByUserIdAndStallId(Long userId, Long stallId);
}
