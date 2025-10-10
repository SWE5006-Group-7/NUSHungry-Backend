package com.nushungry.repository;

import com.nushungry.model.Favorite;
import com.nushungry.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    // 按sortOrder排序查询用户收藏
    List<Favorite> findByUserOrderBySortOrderAsc(User user);

    // 原有方法
    List<Favorite> findByUser(User user);
    Optional<Favorite> findByUserAndStallId(User user, Long stallId);
    boolean existsByUserAndStallId(User user, Long stallId);
    void deleteByUserAndStallId(User user, Long stallId);

    // 批量删除
    void deleteByIdIn(List<Long> ids);

    // 查找用户的特定收藏
    Optional<Favorite> findByIdAndUser(Long id, User user);
}