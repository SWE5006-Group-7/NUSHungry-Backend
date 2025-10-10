package com.nushungry.controller;

import com.nushungry.model.Favorite;
import com.nushungry.model.Stall;
import com.nushungry.service.FavoriteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@CrossOrigin(origins = "*")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping
    public ResponseEntity<Favorite> addFavorite(@RequestBody Map<String, Object> request) {
        Long userId = Long.valueOf(request.get("userId").toString());
        Long stallId = Long.valueOf(request.get("stallId").toString());

        Favorite favorite = favoriteService.addFavorite(userId.toString(), stallId);
        return ResponseEntity.ok(favorite);
    }

    @DeleteMapping
    public ResponseEntity<Void> removeFavorite(@RequestParam String userId, @RequestParam Long stallId) {
        favoriteService.removeFavorite(userId, stallId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Stall>> getUserFavorites(@PathVariable String userId) {
        List<Stall> favorites = favoriteService.getUserFavorites(userId);
        return ResponseEntity.ok(favorites);
    }

    @GetMapping("/check")
    public ResponseEntity<Map<String, Boolean>> checkFavorite(@RequestParam String userId, @RequestParam Long stallId) {
        boolean isFavorite = favoriteService.isFavorite(userId, stallId);
        return ResponseEntity.ok(Map.of("isFavorite", isFavorite));
    }

    /**
     * 获取用户收藏列表（包含完整Favorite信息）
     */
    @GetMapping("/user/{userId}/full")
    public ResponseEntity<List<Favorite>> getUserFavoritesFull(@PathVariable String userId) {
        List<Favorite> favorites = favoriteService.getUserFavoritesFull(userId);
        return ResponseEntity.ok(favorites);
    }

    /**
     * 批量删除收藏
     */
    @DeleteMapping("/batch")
    public ResponseEntity<Void> batchDeleteFavorites(@RequestBody Map<String, Object> request) {
        String userId = request.get("userId").toString();
        @SuppressWarnings("unchecked")
        List<Long> favoriteIds = (List<Long>) request.get("favoriteIds");

        favoriteService.batchDeleteFavorites(userId, favoriteIds);
        return ResponseEntity.noContent().build();
    }

    /**
     * 批量更新收藏排序
     */
    @PutMapping("/reorder")
    public ResponseEntity<Void> reorderFavorites(@RequestBody Map<String, Object> request) {
        String userId = request.get("userId").toString();
        @SuppressWarnings("unchecked")
        List<Long> favoriteIds = (List<Long>) request.get("favoriteIds");

        favoriteService.batchUpdateFavoriteOrder(userId, favoriteIds);
        return ResponseEntity.ok().build();
    }
}