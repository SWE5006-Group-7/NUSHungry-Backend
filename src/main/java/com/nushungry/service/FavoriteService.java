package com.nushungry.service;

import com.nushungry.dto.FavoriteResponse;
import com.nushungry.dto.UpdateFavoriteOrderRequest;
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

    /**
     * 获取用户收藏列表（带排序和详细信息）
     */
    public List<FavoriteResponse> getUserFavoritesDetailed(String userId) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        return favoriteRepository.findByUserOrderBySortOrderDescCreatedAtDesc(user)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 批量删除收藏
     */
    @Transactional
    public void batchDeleteFavorites(String userId, List<Long> favoriteIds) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Favorite> favorites = favoriteRepository.findByIdInAndUser(favoriteIds, user);
        favoriteRepository.deleteAll(favorites);
    }

    /**
     * 更新收藏排序
     */
    @Transactional
    public void updateFavoriteOrders(String userId, List<UpdateFavoriteOrderRequest.FavoriteOrder> orders) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));

        for (UpdateFavoriteOrderRequest.FavoriteOrder order : orders) {
            Favorite favorite = favoriteRepository.findByIdAndUser(order.getFavoriteId(), user)
                    .orElseThrow(() -> new RuntimeException("Favorite not found"));
            favorite.setSortOrder(order.getSortOrder());
            favoriteRepository.save(favorite);
        }
    }

    /**
     * 通过favoriteId删除收藏
     */
    @Transactional
    public void removeFavoriteById(String userId, Long favoriteId) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        favoriteRepository.deleteByIdAndUser(favoriteId, user);
    }

    public boolean isFavorite(String userId, Long stallId) {
        User user = userRepository.findById(Long.parseLong(userId))
                .orElseThrow(() -> new RuntimeException("User not found"));
        return favoriteRepository.existsByUserAndStallId(user, stallId);
    }

    /**
     * 转换Favorite为响应DTO
     */
    private FavoriteResponse convertToResponse(Favorite favorite) {
        Stall stall = favorite.getStall();
        FavoriteResponse response = new FavoriteResponse();

        response.setFavoriteId(favorite.getId());
        response.setStallId(stall.getId());
        response.setStallName(stall.getName());

        // 处理图片列表
        if (stall.getImages() != null && !stall.getImages().isEmpty()) {
            java.util.List<String> imageUrls = stall.getImages().stream()
                    .map(img -> img.getImageUrl())
                    .collect(java.util.stream.Collectors.toList());
            response.setStallImages(imageUrls);
            response.setStallImage(imageUrls.get(0));
        } else if (stall.getImageUrl() != null) {
            // 如果没有images关系，使用单个imageUrl字段
            response.setStallImages(java.util.Collections.singletonList(stall.getImageUrl()));
            response.setStallImage(stall.getImageUrl());
        }

        response.setCuisineType(stall.getCuisineType());
        // 处理halal信息 - halalInfo是String类型，判断是否为"Halal"
        response.setHalal(stall.getHalalInfo() != null &&
                         stall.getHalalInfo().equalsIgnoreCase("Halal"));
        response.setAverageRating(stall.getAverageRating());
        response.setReviewCount(stall.getReviewCount());

        if (stall.getCafeteria() != null) {
            response.setCafeteriaId(stall.getCafeteria().getId());
            response.setCafeteriaName(stall.getCafeteria().getName());
            response.setCafeteriaLocation(stall.getCafeteria().getLocation());
        }

        response.setCreatedAt(favorite.getCreatedAt());
        response.setSortOrder(favorite.getSortOrder());

        return response;
    }
}