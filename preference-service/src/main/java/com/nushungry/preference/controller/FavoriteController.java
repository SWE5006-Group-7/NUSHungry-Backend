package com.nushungry.preference.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.nushungry.preference.service.FavoriteService;
import java.util.List;

@RestController
@RequestMapping("/preference/favorite")
public class FavoriteController {
    @Autowired
    private FavoriteService favoriteService;

    /**
     * Add a favorite record for a user.
     */
    @PostMapping("/add")
    public String addFavorite(@RequestParam Long userId, @RequestParam Long stallId) {
        favoriteService.addFavorite(userId, stallId);
        return "success";
    }

    /**
     * Remove a favorite record for a user.
     */
    @DeleteMapping("/remove")
    public String removeFavorite(@RequestParam Long userId, @RequestParam Long stallId) {
        favoriteService.removeFavorite(userId, stallId);
        return "success";
    }

    /**
     * Get the list of favorite stall IDs for a user.
     */
    @GetMapping("/list")
    public List<Long> listFavorites(@RequestParam Long userId) {
        return favoriteService.listFavorites(userId);
    }

    /**
     * Batch remove favorite records for a user.
     */
    @PostMapping("/batchRemove")
    public String batchRemove(@RequestParam Long userId, @RequestBody List<Long> stallIds) {
        favoriteService.batchRemove(userId, stallIds);
        return "success";
    }

    /**
     * Get the sorted list of favorite stall IDs for a user (by favorite time descending).
     */
    @GetMapping("/sorted")
    public List<Long> sortedFavorites(@RequestParam Long userId) {
        return favoriteService.sortedFavorites(userId);
    }
}
