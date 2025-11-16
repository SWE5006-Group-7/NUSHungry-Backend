package com.nushungry.userservice.controller;

import com.nushungry.userservice.dto.ReviewResponse;
import com.nushungry.userservice.dto.UserProfileResponse;
import com.nushungry.userservice.model.User;
import com.nushungry.userservice.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户个人中心控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Tag(name = "User", description = "用户个人中心接口")
@SecurityRequirement(name = "Bearer Authentication")
public class UserController {

    private final UserService userService;
    private final RestTemplate restTemplate;

    @Value("${services.review-service.url}")
    private String reviewServiceUrl;

    @Value("${services.preference-service.url}")
    private String preferenceServiceUrl;

    @Value("${services.media-service.url}")
    private String mediaServiceUrl;

    /**
     * 获取当前用户资料
     */
    @GetMapping("/profile")
    @Operation(summary = "获取当前用户资料")
    public ResponseEntity<UserProfileResponse> getProfile() {
        log.info("Getting current user profile");
        UserProfileResponse profile = userService.getCurrentUserProfile();
        return ResponseEntity.ok(profile);
    }

    /**
     * 获取当前用户的评价列表
     */
    @GetMapping("/reviews")
    @Operation(summary = "获取当前用户的评价列表")
    public ResponseEntity<List<ReviewResponse>> getUserReviews() {
        User user = userService.getCurrentUser();
        log.info("Getting reviews for user: {}", user.getId());

        try {
            // 调用 review-service 获取用户评价
            String url = reviewServiceUrl + "/reviews/user/" + user.getId();

            ResponseEntity<List<ReviewResponse>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<ReviewResponse>>() {}
            );

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Error calling review-service: {}", e.getMessage());
            // 返回空列表而不是错误，避免前端异常
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 获取当前用户的收藏列表
     */
    @GetMapping("/favorites")
    @Operation(summary = "获取当前用户的收藏列表")
    public ResponseEntity<List<Map<String, Object>>> getUserFavorites() {
        User user = userService.getCurrentUser();
        log.info("Getting favorites for user: {}", user.getId());

        try {
            // 调用 preference-service 获取收藏列表
            String url = preferenceServiceUrl + "/favorites/user/" + user.getId() + "/detailed";

            ResponseEntity<List<Map<String, Object>>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<Map<String, Object>>>() {}
            );

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Error calling preference-service: {}", e.getMessage());
            // 返回空列表而不是错误，避免前端异常
            return ResponseEntity.ok(List.of());
        }
    }

    /**
     * 上传用户头像
     * 支持可选的裁剪参数（1:1比例）
     */
    @PostMapping("/avatar")
    @Operation(summary = "上传用户头像")
    public ResponseEntity<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "x", required = false, defaultValue = "0") int x,
            @RequestParam(value = "y", required = false, defaultValue = "0") int y,
            @RequestParam(value = "width", required = false, defaultValue = "0") int width,
            @RequestParam(value = "height", required = false, defaultValue = "0") int height) {

        User user = userService.getCurrentUser();
        log.info("Uploading avatar for user: {}", user.getId());

        try {
            // 构建 multipart 请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // 创建请求体
            org.springframework.util.LinkedMultiValueMap<String, Object> body = new org.springframework.util.LinkedMultiValueMap<>();
            body.add("file", file.getResource());

            // 如果有裁剪参数，添加到请求中
            if (width > 0 && height > 0) {
                body.add("x", String.valueOf(x));
                body.add("y", String.valueOf(y));
                body.add("width", String.valueOf(width));
                body.add("height", String.valueOf(height));
                body.add("crop", "true");
            }

            HttpEntity<org.springframework.util.LinkedMultiValueMap<String, Object>> requestEntity =
                    new HttpEntity<>(body, headers);

            // 调用 media-service 上传图片
            String url = mediaServiceUrl + "/upload/image";

            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                String avatarUrl = (String) response.getBody().get("url");

                // 更新用户头像URL
                userService.updateAvatar(avatarUrl);

                Map<String, String> result = new HashMap<>();
                result.put("avatarUrl", avatarUrl);
                return ResponseEntity.ok(result);
            } else {
                log.error("Failed to upload avatar to media-service");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }
        } catch (Exception e) {
            log.error("Error uploading avatar: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
