package com.nushungry.cafeteriaservice.controller;

import com.nushungry.cafeteriaservice.dto.StallSearchRequest;
import com.nushungry.cafeteriaservice.model.Stall;
import com.nushungry.cafeteriaservice.service.StallService;
import com.nushungry.cafeteriaservice.specification.StallSpecification;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stalls")
public class StallController {

    private final StallService stallService;

    public StallController(StallService stallService) {
        this.stallService = stallService;
    }

    @GetMapping
    public List<Map<String, Object>> getAllStalls() {
        List<Stall> stalls = stallService.findAll();
        return stalls.stream().map(this::buildStallResponse).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getStall(@PathVariable Long id) {
        return stallService.findById(id)
            .map(stall -> ResponseEntity.ok(buildStallResponse(stall)))
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 搜索和筛选摊位
     * GET /api/stalls/search?keyword=chicken&cuisineTypes=Chinese,Western&minRating=4.0&sortBy=rating
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchStalls(
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

        // 构建响应数据（包含距离信息）
        List<Map<String, Object>> stallDataList = pageResult.getContent().stream()
            .map(stall -> {
                Map<String, Object> stallData = buildStallResponse(stall);

                // 添加距离信息（如果提供了用户位置）
                if (userLatitude != null && userLongitude != null) {
                    double lat = stall.getLatitude() != null ? stall.getLatitude()
                        : (stall.getCafeteria() != null ? stall.getCafeteria().getLatitude() : 0);
                    double lon = stall.getLongitude() != null ? stall.getLongitude()
                        : (stall.getCafeteria() != null ? stall.getCafeteria().getLongitude() : 0);

                    double distance = StallSpecification.calculateDistance(
                        userLatitude, userLongitude, lat, lon);
                    stallData.put("distance", String.format("%.1f km", distance));
                    stallData.put("distanceValue", distance);
                } else {
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

    /**
     * 构建摊位响应数据（解决@JsonBackReference循环引用问题）
     */
    private Map<String, Object> buildStallResponse(Stall stall) {
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
        stallData.put("latitude", stall.getLatitude());
        stallData.put("longitude", stall.getLongitude());

        // 添加cafeteria信息（解决@JsonBackReference问题）
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
        } else {
            stallData.put("cafeteria", null);
            stallData.put("cafeteriaName", null);
            stallData.put("cafeteriaId", null);
        }

        return stallData;
    }
}


