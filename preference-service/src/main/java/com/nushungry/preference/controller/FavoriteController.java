package com.nushungry.preference.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import com.nushungry.preference.dto.FavoriteResponse;
import com.nushungry.preference.dto.UpdateFavoriteOrderRequest;
import com.nushungry.preference.service.FavoriteService;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {
    @Autowired
    private FavoriteService favoriteService;

    /**
     * Add a favorite record for a user.
     */
    @PostMapping
    public ResponseEntity<String> addFavorite(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        Long stallId = Long.valueOf(request.get("stallId").toString());

        favoriteService.addFavorite(userId, stallId);
        return ResponseEntity.ok("success");
    }

    /**
     * Remove a favorite record for a user.
     */
    @DeleteMapping
    public ResponseEntity<String> removeFavorite(@RequestParam Long userId, @RequestParam Long stallId) {
        favoriteService.removeFavorite(userId, stallId);
        return ResponseEntity.ok("success");
    }

    /**
     * Get the list of favorite stall IDs for a user.
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Long>> listFavorites(@PathVariable Long userId) {
        List<Long> favorites = favoriteService.listFavorites(userId);
        return ResponseEntity.ok(favorites);
    }

    /**
     * Batch remove favorite records for a user.
     */
    @PostMapping("/batchRemove")
    public ResponseEntity<String> batchRemove(@RequestParam Long userId, @RequestBody List<Long> stallIds) {
        favoriteService.batchRemove(userId, stallIds);
        return ResponseEntity.ok("success");
    }

    /**
     * Get the sorted list of favorite stall IDs for a user.
     */
    @GetMapping("/sorted")
    public ResponseEntity<List<Long>> sortedFavorites(@RequestParam Long userId) {
        List<Long> favorites = favoriteService.sortedFavorites(userId);
        return ResponseEntity.ok(favorites);
    }

    /**
     * 检查是否已收藏
     */
    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(
            @RequestParam Long userId,
            @RequestParam Long stallId) {
        boolean isFavorite = favoriteService.isFavorite(userId, stallId);
        return ResponseEntity.ok(Map.of("isFavorite", isFavorite));
    }

    /**
     * 获取详细收藏列表
     */
    @GetMapping("/user/{userId}/detailed")
    public ResponseEntity<List<FavoriteResponse>> getUserFavoritesDetailed(@PathVariable Long userId) {
        List<FavoriteResponse> favorites = favoriteService.getUserFavoritesDetailed(userId);
        return ResponseEntity.ok(favorites);
    }

    /**
     * 更新收藏排序
     */
    @PutMapping("/order")
    public ResponseEntity<Void> updateFavoriteOrders(
            @RequestParam Long userId,
            @RequestBody UpdateFavoriteOrderRequest request) {
        favoriteService.updateFavoriteOrders(userId, request.getOrders());
        return ResponseEntity.ok().build();
    }

    /**
     * 通过ID删除收藏
     */
    @DeleteMapping("/{favoriteId}")
    public ResponseEntity<Void> removeFavoriteById(
            @PathVariable Long favoriteId,
            @RequestParam Long userId) {
        favoriteService.removeFavoriteById(userId, favoriteId);
        return ResponseEntity.noContent().build();
    }
}