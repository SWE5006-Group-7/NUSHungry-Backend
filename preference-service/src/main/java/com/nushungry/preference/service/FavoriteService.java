package com.nushungry.preference.service;

import com.nushungry.preference.entity.Favorite;
import com.nushungry.preference.repository.FavoriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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
        List<Favorite> favs = favoriteRepository.findByUserId(userId);
        favs.sort((a, b) -> Long.compare(b.getCreatedAt(), a.getCreatedAt()));
        List<Long> result = new ArrayList<>();
        for (Favorite f : favs) {
            result.add(f.getStallId());
        }
        return result;
    }
}
