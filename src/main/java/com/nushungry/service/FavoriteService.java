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
        return favoriteRepository.findByUser(user)
                .stream()
                .map(Favorite::getStall)
                .collect(Collectors.toList());
    }

    public boolean isFavorite(String userId, Long stallId) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        return favoriteRepository.existsByUserAndStallId(user, stallId);
    }
}