package com.nushungry.controller;

import com.nushungry.dto.StallSearchRequest;
import com.nushungry.model.Stall;
import com.nushungry.model.StallDetailDTO;
import com.nushungry.service.StallService;
import com.nushungry.service.ImageService;
import com.nushungry.service.SearchHistoryService;
import com.nushungry.specification.StallSpecification;
import com.nushungry.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stalls")
public class StallController {

    @Autowired
    private StallService stallService;

    @Autowired
    private ImageService imageService;

    @Autowired
    private SearchHistoryService searchHistoryService;

    @Autowired
    private JwtUtil jwtUtil;

    @GetMapping
    public List<Map<String, Object>> getAllStalls() {
        List<Stall> stalls = stallService.findAll();
        // 手动构建包含cafeteria信息的响应，解决@JsonBackReference序列化问题
        return stalls.stream().map(stall -> {
            Map<String, Object> stallData = new HashMap<>();
            stallData.put("id", stall.getId());
            stallData.put("name", stall.getName());
            stallData.put("cuisineType", stall.getCuisineType());
            stallData.put("cuisine", stall.getCuisineType()); // 前端使用cuisine字段
            stallData.put("imageUrl", stall.getImageUrl());
            stallData.put("halalInfo", stall.getHalalInfo());
            stallData.put("halal", stall.getHalalInfo() != null && !stall.getHalalInfo().isEmpty());
            stallData.put("contact", stall.getContact());
            stallData.put("averageRating", stall.getAverageRating());
            stallData.put("reviewCount", stall.getReviewCount());
            stallData.put("averagePrice", stall.getAveragePrice());

            // 添加stall自己的坐标
            stallData.put("latitude", stall.getLatitude());
            stallData.put("longitude", stall.getLongitude());

            // 添加cafeteria信息
            if (stall.getCafeteria() != null) {
                Map<String, Object> cafeteriaData = new HashMap<>();
                cafeteriaData.put("id", stall.getCafeteria().getId());
                cafeteriaData.put("name", stall.getCafeteria().getName());
                cafeteriaData.put("location", stall.getCafeteria().getLocation());
                stallData.put("cafeteria", cafeteriaData);
                stallData.put("cafeteriaName", stall.getCafeteria().getName()); // 前端使用cafeteriaName字段
                stallData.put("cafeteriaId", stall.getCafeteria().getId()); // 添加cafeteriaId供前端判断
            } else {
                stallData.put("cafeteria", null);
                stallData.put("cafeteriaName", null);
                stallData.put("cafeteriaId", null);
            }

            return stallData;
        }).collect(Collectors.toList());
    }

    /**
     * 搜索和筛选摊位
     * GET /api/stalls/search?keyword=chicken&cuisineTypes=Chinese,Western&minRating=4.0&sortBy=rating
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchStalls(
        HttpServletRequest httpRequest,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) List<String> cuisineTypes,
        @RequestParam(required = false) Double minRating,
        @RequestParam(required = false) Boolean halalOnly,
        @RequestParam(required = false) Long cafeteriaId,
        @RequestParam(required = false) Double userLatitude,
        @RequestParam(required = false) Double userLongitude,
        @RequestParam(required = false) Double maxDistance,
        @RequestParam(required = false, defaultValue = "rating") String sortBy,
        @RequestParam(required = false, defaultValue = "desc") String sortDirection,
        @RequestParam(required = false, defaultValue = "0") Integer page,
        @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        // 构建搜索请求
        StallSearchRequest request = new StallSearchRequest();
        request.setKeyword(keyword);
        request.setCuisineTypes(cuisineTypes);
        request.setMinRating(minRating);
        request.setHalalOnly(halalOnly);
        request.setCafeteriaId(cafeteriaId);
        request.setUserLatitude(userLatitude);
        request.setUserLongitude(userLongitude);
        request.setMaxDistance(maxDistance);
        request.setSortBy(sortBy);
        request.setSortDirection(sortDirection);
        request.setPage(page);
        request.setSize(size);

        // 执行搜索
        Page<Stall> pageResult = stallService.searchStalls(request);

        // 记录搜索历史（异步，仅在有关键词时）
        if (StringUtils.hasText(keyword)) {
            Long userId = getUserIdFromRequest(httpRequest);
            System.out.println(">>> StallController: 准备记录搜索历史");
            System.out.println(">>> keyword: " + keyword);
            System.out.println(">>> userId: " + userId);
            searchHistoryService.recordSearch(
                userId,
                keyword,
                "stall",
                (int) pageResult.getTotalElements(),
                httpRequest
            );
            System.out.println(">>> StallController: recordSearch方法已调用");
        } else {
            System.out.println(">>> StallController: keyword为空，不记录搜索历史");
        }

        // 构建响应数据（包含距离信息）
        List<Map<String, Object>> stallDataList = pageResult.getContent().stream()
            .map(stall -> {
                Map<String, Object> stallData = new HashMap<>();
                stallData.put("id", stall.getId());
                stallData.put("name", stall.getName());
                stallData.put("cuisineType", stall.getCuisineType());
                stallData.put("cuisine", stall.getCuisineType());
                stallData.put("imageUrl", stall.getImageUrl());
                stallData.put("halalInfo", stall.getHalalInfo());
                stallData.put("halal", stall.getHalalInfo() != null && !stall.getHalalInfo().isEmpty());
                stallData.put("contact", stall.getContact());
                stallData.put("averageRating", stall.getAverageRating());
                stallData.put("reviewCount", stall.getReviewCount());
                stallData.put("averagePrice", stall.getAveragePrice());

                // 添加stall自己的坐标
                stallData.put("latitude", stall.getLatitude());
                stallData.put("longitude", stall.getLongitude());

                // 添加cafeteria信息和距离
                if (stall.getCafeteria() != null) {
                    Map<String, Object> cafeteriaData = new HashMap<>();
                    cafeteriaData.put("id", stall.getCafeteria().getId());
                    cafeteriaData.put("name", stall.getCafeteria().getName());
                    cafeteriaData.put("location", stall.getCafeteria().getLocation());
                    cafeteriaData.put("latitude", stall.getCafeteria().getLatitude());
                    cafeteriaData.put("longitude", stall.getCafeteria().getLongitude());
                    stallData.put("cafeteria", cafeteriaData);
                    stallData.put("cafeteriaName", stall.getCafeteria().getName());
                    stallData.put("cafeteriaId", stall.getCafeteria().getId());

                    // 计算距离（如果提供了用户位置）
                    if (userLatitude != null && userLongitude != null) {
                        double distance = StallSpecification.calculateDistance(
                            userLatitude, userLongitude,
                            stall.getCafeteria().getLatitude(),
                            stall.getCafeteria().getLongitude()
                        );
                        stallData.put("distance", String.format("%.1f km", distance));
                        stallData.put("distanceValue", distance);
                    } else {
                        stallData.put("distance", "N/A");
                        stallData.put("distanceValue", null);
                    }
                } else {
                    stallData.put("cafeteria", null);
                    stallData.put("cafeteriaName", null);
                    stallData.put("cafeteriaId", null);
                    stallData.put("distance", "N/A");
                    stallData.put("distanceValue", null);
                }

                return stallData;
            })
            .collect(Collectors.toList());

        // 构建分页响应
        Map<String, Object> response = new HashMap<>();
        response.put("content", stallDataList);
        response.put("totalElements", pageResult.getTotalElements());
        response.put("totalPages", pageResult.getTotalPages());
        response.put("currentPage", pageResult.getNumber());
        response.put("pageSize", pageResult.getSize());
        response.put("hasNext", pageResult.hasNext());
        response.put("hasPrevious", pageResult.hasPrevious());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StallDetailDTO> getStallById(@PathVariable Long id) {
        return stallService.findById(id)
                .map(stall -> {
                    StallDetailDTO dto = new StallDetailDTO(
                        stall.getId(),
                        stall.getName(),
                        stall.getCuisineType(),
                        stall.getImageUrl(),
                        stall.getHalalInfo(),
                        stall.getContact()
                    );

                    // 设置stall自己的坐标
                    dto.setLatitude(stall.getLatitude());
                    dto.setLongitude(stall.getLongitude());
                    
                    // 设置评分和价格信息
                    dto.setAverageRating(stall.getAverageRating());
                    dto.setReviewCount(stall.getReviewCount());
                    dto.setAveragePrice(stall.getAveragePrice());

                    // 设置 cafeteria 的完整信息,包含坐标
                    if (stall.getCafeteria() != null) {
                        dto.setCafeteriaId(stall.getCafeteria().getId());
                        dto.setCafeteriaName(stall.getCafeteria().getName());

                        // 创建cafeteria完整信息对象
                        StallDetailDTO.CafeteriaBasicDTO cafeteriaDTO = new StallDetailDTO.CafeteriaBasicDTO(
                            stall.getCafeteria().getId(),
                            stall.getCafeteria().getName(),
                            stall.getCafeteria().getLocation(),
                            stall.getCafeteria().getLatitude(),
                            stall.getCafeteria().getLongitude()
                        );
                        dto.setCafeteria(cafeteriaDTO);
                    }

                    dto.setReviews(stall.getReviews());
                    dto.setImages(imageService.getStallImages(id));
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Stall createStall(@RequestBody Stall stall) {
        return stallService.save(stall);
    }

    /**
     * 从请求中提取用户ID（可能为null，表示匿名用户）
     */
    private Long getUserIdFromRequest(HttpServletRequest request) {
        try {
            String token = extractToken(request);
            if (token != null && jwtUtil.validateToken(token)) {
                return jwtUtil.extractUserId(token);
            }
        } catch (Exception e) {
            // 忽略异常，返回null表示匿名用户
        }
        return null;
    }

    /**
     * 从请求头中提取JWT token
     */
    private String extractToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

}
