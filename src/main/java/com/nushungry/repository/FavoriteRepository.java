package com.nushungry.repository;

import com.nushungry.model.Favorite;
import com.nushungry.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUserOrderBySortOrderDescCreatedAtDesc(User user);
    List<Favorite> findByUser(User user);
    Optional<Favorite> findByUserAndStallId(User user, Long stallId);
    Optional<Favorite> findByIdAndUser(Long id, User user);
    boolean existsByUserAndStallId(User user, Long stallId);
    void deleteByUserAndStallId(User user, Long stallId);
    void deleteByIdAndUser(Long id, User user);
    List<Favorite> findByIdInAndUser(List<Long> ids, User user);
}