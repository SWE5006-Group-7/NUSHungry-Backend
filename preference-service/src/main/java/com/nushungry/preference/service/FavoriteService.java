package com.nushungry.preference.service;

import com.nushungry.preference.dto.FavoriteResponse;
import com.nushungry.preference.entity.Favorite;
import com.nushungry.preference.repository.FavoriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 收藏服务层
 * 
 * 缓存策略：
 * - listFavorites(): 缓存用户收藏列表（favorites::{userId}）
 * - sortedFavorites(): 缓存排序后的收藏列表（favorites::sorted::{userId}）
 * - addFavorite(): 清除用户收藏缓存
 * - removeFavorite(): 清除用户收藏缓存
 * - batchRemove(): 清除用户收藏缓存
 */
@Service
public class FavoriteService {
    @Autowired
    private FavoriteRepository favoriteRepository;

    /**
     * Add a favorite record for a user.
     * 清除该用户的所有收藏缓存
     */
    @Caching(evict = {
        @CacheEvict(value = "favorites", key = "#userId"),
        @CacheEvict(value = "favorites", key = "'sorted::' + #userId")
    })
    public void addFavorite(Long userId, Long stallId) {
        if (!favoriteRepository.existsByUserIdAndStallId(userId, stallId)) {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setStallId(stallId);
            favorite.setCreatedAt(System.currentTimeMillis());
            favorite.setSortOrder(0); // 默认排序为0
            favoriteRepository.save(favorite);
        }
    }

    /**
     * Remove a favorite record for a user.
     * 清除该用户的所有收藏缓存
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "favorites", key = "#userId"),
        @CacheEvict(value = "favorites", key = "'sorted::' + #userId")
    })
    public void removeFavorite(Long userId, Long stallId) {
        favoriteRepository.deleteByUserIdAndStallId(userId, stallId);
    }

    /**
     * Get the list of favorite stall IDs for a user.
     * 缓存用户收藏列表，TTL 10分钟
     */
    @Cacheable(value = "favorites", key = "#userId", unless = "#result == null || #result.isEmpty()")
    public List<Long> listFavorites(Long userId) {
        List<Favorite> favs = favoriteRepository.findByUserId(userId);
        List<Long> result = new ArrayList<>();
        for (Favorite f : favs) {
            result.add(f.getStallId());
        }
        return result;
    }

    /**
     * Batch remove favorite records for a user.
     * 清除该用户的所有收藏缓存
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "favorites", key = "#userId"),
        @CacheEvict(value = "favorites", key = "'sorted::' + #userId")
    })
    public void batchRemove(Long userId, List<Long> stallIds) {
        favoriteRepository.deleteByUserIdAndStallIdIn(userId, stallIds);
    }

    /**
     * Get the sorted list of favorite stall IDs for a user (by favorite time descending).
     * 缓存排序后的收藏列表，TTL 10分钟
     */
    @Cacheable(value = "favorites", key = "'sorted::' + #userId", unless = "#result == null || #result.isEmpty()")
    public List<Long> sortedFavorites(Long userId) {
        List<Favorite> favs = favoriteRepository.findByUserIdOrderBySortOrderDescCreatedAtDesc(userId);
        List<Long> result = new ArrayList<>();
        for (Favorite f : favs) {
            result.add(f.getStallId());
        }
        return result;
    }

    /**
     * 检查收藏状态
     */
    @Cacheable(value = "favorites", key = "'check::' + #userId + '::' + #stallId")
    public boolean isFavorite(Long userId, Long stallId) {
        return favoriteRepository.existsByUserIdAndStallId(userId, stallId);
    }

    /**
     * 获取详细收藏列表（包含摊位信息）
     * 注意：这里简化实现，实际需要调用 cafeteria-service 获取摊位详情
     */
    public List<FavoriteResponse> getUserFavoritesDetailed(Long userId) {
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderBySortOrderDescCreatedAtDesc(userId);
        List<FavoriteResponse> responses = new ArrayList<>();

        for (Favorite fav : favorites) {
            FavoriteResponse response = new FavoriteResponse();
            response.setFavoriteId(fav.getId());
            response.setStallId(fav.getStallId());
            response.setSortOrder(fav.getSortOrder());
            response.setCreatedAt(LocalDateTime.now()); // 这里应该是从timestamp转换，简化处理

            // TODO: 调用 cafeteria-service 获取摊位详细信息
            // response.setStallName(...);
            // response.setStallImage(...);
            // response.setCuisineType(...);

            responses.add(response);
        }

        return responses;
    }

    /**
     * 更新收藏排序
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "favorites", key = "#userId"),
        @CacheEvict(value = "favorites", key = "'sorted::' + #userId"),
        @CacheEvict(value = "favorites", allEntries = true, key = "'check::*'")
    })
    public void updateFavoriteOrders(Long userId, Map<Long, Integer> orders) {
        for (Map.Entry<Long, Integer> entry : orders.entrySet()) {
            Long favoriteId = entry.getKey();
            Integer sortOrder = entry.getValue();

            // 这里需要确保只能更新自己的收藏
            // 实际实现中需要验证 favoriteId 是否属于该用户
            favoriteRepository.findById(favoriteId).ifPresent(favorite -> {
                if (favorite.getUserId().equals(userId)) {
                    favorite.setSortOrder(sortOrder);
                    favoriteRepository.save(favorite);
                }
            });
        }
    }

    /**
     * 通过favoriteId删除收藏
     */
    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "favorites", key = "#userId"),
        @CacheEvict(value = "favorites", key = "'sorted::' + #userId"),
        @CacheEvict(value = "favorites", allEntries = true, key = "'check::*'")
    })
    public void removeFavoriteById(Long userId, Long favoriteId) {
        favoriteRepository.deleteByIdAndUser(favoriteId, userId);
    }
}
