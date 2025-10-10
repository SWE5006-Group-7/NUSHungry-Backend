package com.nushungry.service;

import com.nushungry.model.Favorite;
import com.nushungry.model.Stall;
import com.nushungry.model.User;
import com.nushungry.repository.FavoriteRepository;
import com.nushungry.repository.StallRepository;
import com.nushungry.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final StallRepository stallRepository;
    private final UserRepository userRepository;

    public FavoriteService(FavoriteRepository favoriteRepository, StallRepository stallRepository, UserRepository userRepository) {
        this.favoriteRepository = favoriteRepository;
        this.stallRepository = stallRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Favorite addFavorite(String userId, Long stallId) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (favoriteRepository.existsByUserAndStallId(user, stallId)) {
            throw new RuntimeException("Stall is already in favorites");
        }

        Stall stall = stallRepository.findById(stallId)
                .orElseThrow(() -> new RuntimeException("Stall not found"));

        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setStall(stall);

        return favoriteRepository.save(favorite);
    }

    @Transactional
    public void removeFavorite(String userId, Long stallId) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        favoriteRepository.deleteByUserAndStallId(user, stallId);
    }

    public List<Stall> getUserFavorites(String userId) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        return favoriteRepository.findByUserOrderBySortOrderAsc(user)
                .stream()
                .map(Favorite::getStall)
                .collect(Collectors.toList());
    }

    /**
     * 获取用户收藏列表（包含完整Favorite对象，用于排序等）
     */
    public List<Favorite> getUserFavoritesFull(String userId) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        return favoriteRepository.findByUserOrderBySortOrderAsc(user);
    }

    /**
     * 批量删除收藏
     */
    @Transactional
    public void batchDeleteFavorites(String userId, List<Long> favoriteIds) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 验证所有收藏都属于该用户
        for (Long id : favoriteIds) {
            favoriteRepository.findByIdAndUser(id, user)
                    .orElseThrow(() -> new RuntimeException("Favorite not found or not owned by user: " + id));
        }

        favoriteRepository.deleteByIdIn(favoriteIds);
    }

    /**
     * 更新收藏排序
     */
    @Transactional
    public void updateFavoriteOrder(String userId, Long favoriteId, Integer newSortOrder) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        Favorite favorite = favoriteRepository.findByIdAndUser(favoriteId, user)
                .orElseThrow(() -> new RuntimeException("Favorite not found or not owned by user"));

        favorite.setSortOrder(newSortOrder);
        favoriteRepository.save(favorite);
    }

    /**
     * 批量更新收藏排序
     */
    @Transactional
    public void batchUpdateFavoriteOrder(String userId, List<Long> favoriteIds) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        // 按传入的顺序更新sortOrder
        for (int i = 0; i < favoriteIds.size(); i++) {
            Long favoriteId = favoriteIds.get(i);
            Favorite favorite = favoriteRepository.findByIdAndUser(favoriteId, user)
                    .orElseThrow(() -> new RuntimeException("Favorite not found or not owned by user: " + favoriteId));
            favorite.setSortOrder(i);
            favoriteRepository.save(favorite);
        }
    }

    public boolean isFavorite(String userId, Long stallId) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        return favoriteRepository.existsByUserAndStallId(user, stallId);
    }
}