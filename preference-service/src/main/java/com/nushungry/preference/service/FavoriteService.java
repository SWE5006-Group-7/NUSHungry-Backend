package com.nushungry.preference.service;

import com.nushungry.preference.entity.Favorite;
import com.nushungry.preference.repository.FavoriteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class FavoriteService {
    @Autowired
    private FavoriteRepository favoriteRepository;

    /**
     * Add a favorite record for a user.
     */
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
     */
    public void removeFavorite(Long userId, Long stallId) {
        favoriteRepository.deleteByUserIdAndStallId(userId, stallId);
    }

    /**
     * Get the list of favorite stall IDs for a user.
     */
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
     */
    @Transactional
    public void batchRemove(Long userId, List<Long> stallIds) {
        favoriteRepository.deleteByUserIdAndStallIdIn(userId, stallIds);
    }

    /**
     * Get the sorted list of favorite stall IDs for a user (by favorite time descending).
     */
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
