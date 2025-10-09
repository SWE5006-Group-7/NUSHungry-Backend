package com.nushungry.controller;

import com.nushungry.dto.UserProfileResponse;
import com.nushungry.model.Favorite;
import com.nushungry.model.Review;
import com.nushungry.model.User;
import com.nushungry.repository.FavoriteRepository;
import com.nushungry.repository.ReviewRepository;
import com.nushungry.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "User profile and data endpoints")
public class UserController {

    private final UserService userService;
    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;

    @GetMapping("/profile")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<UserProfileResponse> getProfile() {
        UserProfileResponse profile = userService.getCurrentUserProfile();
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/reviews")
    @Operation(summary = "Get current user's reviews")
    public ResponseEntity<List<Review>> getUserReviews() {
        User user = userService.getCurrentUser();
        List<Review> reviews = reviewRepository.findAll().stream()
                .filter(r -> r.getUser() != null && r.getUser().getId().equals(user.getId()))
                .toList();
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/favorites")
    @Operation(summary = "Get current user's favorite stalls")
    public ResponseEntity<List<Favorite>> getUserFavorites() {
        User user = userService.getCurrentUser();
        List<Favorite> favorites = favoriteRepository.findByUser(user);
        return ResponseEntity.ok(favorites);
    }

    @PostMapping("/avatar")
    @Operation(summary = "Upload user avatar")
    public ResponseEntity<Map<String, String>> uploadAvatar(@RequestParam("file") MultipartFile file) {
        try {
            String avatarUrl = userService.uploadAvatar(file);
            Map<String, String> response = new HashMap<>();
            response.put("avatarUrl", avatarUrl);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
