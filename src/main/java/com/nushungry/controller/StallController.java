package com.nushungry.controller;

import com.nushungry.dto.StallSearchRequest;
import com.nushungry.model.Cafeteria;
import com.nushungry.model.Stall;
import com.nushungry.model.StallDetailDTO;
import com.nushungry.service.CafeteriaService;
import com.nushungry.service.StallService;
import com.nushungry.service.ImageService;
import com.nushungry.service.SearchHistoryService;
import com.nushungry.specification.StallSpecification;
import com.nushungry.util.JwtUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 档口管理控制器
 * 提供公开查询接口和管理员专用的CRUD操作接口
 */
@Slf4j
@RestController
@RequestMapping("/api/stalls")
@RequiredArgsConstructor
@Tag(name = "Stall Management", description = "档口管理接口")
public class StallController {

    private final StallService stallService;
    private final CafeteriaService cafeteriaService;
    private final ImageService imageService;
    private final SearchHistoryService searchHistoryService;
    private final JwtUtil jwtUtil;

    // ============ 公开接口（无需认证） ============

    /**
     * 获取所有档口列表（公开接口）
     */
    @GetMapping
    @Operation(summary = "获取所有档口", description = "查询所有档口的基本信息列表")
    public ResponseEntity<List<Map<String, Object>>> getAllStalls() {
        log.info("查询所有档口列表");
        List<Stall> stalls = stallService.findAll();

        // 手动构建包含cafeteria信息的响应，解决@JsonBackReference序列化问题
        List<Map<String, Object>> stallDataList = stalls.stream()
            .map(this::buildStallResponse)
            .collect(Collectors.toList());

        return ResponseEntity.ok(stallDataList);
    }

    /**
     * 搜索和筛选档口（公开接口）
     */
    @GetMapping("/search")
    @Operation(summary = "搜索档口", description = "根据关键词、菜系类型、评分等条件搜索和筛选档口")
    public ResponseEntity<Map<String, Object>> searchStalls(
        HttpServletRequest httpRequest,
        @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
        @Parameter(description = "菜系类型列表") @RequestParam(required = false) List<String> cuisineTypes,
        @Parameter(description = "最低评分") @RequestParam(required = false) Double minRating,
        @Parameter(description = "仅显示Halal档口") @RequestParam(required = false) Boolean halalOnly,
        @Parameter(description = "食堂ID筛选") @RequestParam(required = false) Long cafeteriaId,
        @Parameter(description = "用户纬度") @RequestParam(required = false) Double userLatitude,
        @Parameter(description = "用户经度") @RequestParam(required = false) Double userLongitude,
        @Parameter(description = "最大距离(km)") @RequestParam(required = false) Double maxDistance,
        @Parameter(description = "排序字段") @RequestParam(required = false, defaultValue = "rating") String sortBy,
        @Parameter(description = "排序方向") @RequestParam(required = false, defaultValue = "desc") String sortDirection,
        @Parameter(description = "页码") @RequestParam(required = false, defaultValue = "0") Integer page,
        @Parameter(description = "每页大小") @RequestParam(required = false, defaultValue = "20") Integer size
    ) {
        log.info("搜索档口: keyword={}, cuisineTypes={}, minRating={}, cafeteriaId={}",
                keyword, cuisineTypes, minRating, cafeteriaId);

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
            log.debug("记录搜索历史: keyword={}, userId={}", keyword, userId);
            searchHistoryService.recordSearch(
                userId,
                keyword,
                "stall",
                (int) pageResult.getTotalElements(),
                httpRequest
            );
        }

        // 构建响应数据（包含距离信息）
        List<Map<String, Object>> stallDataList = pageResult.getContent().stream()
            .map(stall -> {
                Map<String, Object> stallData = buildStallResponse(stall);

                // 添加距离信息（如果提供了用户位置）
                if (userLatitude != null && userLongitude != null && stall.getCafeteria() != null) {
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
     * 根据ID获取档口详细信息（公开接口）
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取档口详情", description = "根据ID查询档口详细信息（含评论、图片）")
    public ResponseEntity<StallDetailDTO> getStallById(
            @Parameter(description = "档口ID") @PathVariable Long id) {
        log.info("查询档口详情: ID={}", id);

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

                    // 设置档口自己的坐标
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

    // ============ 管理员专用接口（需要 ADMIN 权限） ============

    /**
     * 创建新档口（管理员专用）
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "创建档口 [管理员]", description = "创建新的档口信息（需要管理员权限）")
    public ResponseEntity<Map<String, Object>> createStall(
            @RequestBody Map<String, Object> requestBody) {
        try {
            log.info("管理员创建新档口: {}", requestBody.get("name"));

            // 构建Stall对象
            Stall stall = new Stall();
            stall.setName((String) requestBody.get("name"));
            stall.setCuisineType((String) requestBody.get("cuisineType"));
            stall.setHalalInfo((String) requestBody.get("halalInfo"));
            stall.setContact((String) requestBody.get("contact"));
            stall.setImageUrl((String) requestBody.get("imageUrl"));

            // 设置坐标信息
            if (requestBody.containsKey("latitude") && requestBody.get("latitude") != null) {
                stall.setLatitude(Double.parseDouble(requestBody.get("latitude").toString()));
            }
            if (requestBody.containsKey("longitude") && requestBody.get("longitude") != null) {
                stall.setLongitude(Double.parseDouble(requestBody.get("longitude").toString()));
            }

            // 关联食堂
            Long cafeteriaId = requestBody.get("cafeteriaId") != null
                ? Long.parseLong(requestBody.get("cafeteriaId").toString())
                : null;

            if (cafeteriaId != null) {
                Optional<Cafeteria> cafeteria = cafeteriaService.findById(cafeteriaId);
                if (cafeteria.isPresent()) {
                    stall.setCafeteria(cafeteria.get());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "食堂不存在 (ID: " + cafeteriaId + ")");
                    return ResponseEntity.badRequest().body(error);
                }
            }

            // 保存档口信息
            Stall savedStall = stallService.save(stall);

            // 使用辅助方法构建包含cafeteria信息的响应
            Map<String, Object> stallData = buildStallResponse(savedStall);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "档口创建成功");
            response.put("stall", stallData);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("创建档口失败: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error("创建档口时发生异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 更新档口信息（管理员专用）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "更新档口信息 [管理员]", description = "更新指定ID的档口信息（需要管理员权限）")
    public ResponseEntity<Map<String, Object>> updateStall(
            @Parameter(description = "档口ID") @PathVariable Long id,
            @RequestBody Map<String, Object> requestBody) {
        try {
            log.info("管理员更新档口: ID={}, 名称={}", id, requestBody.get("name"));

            // 检查档口是否存在
            Optional<Stall> existingStall = stallService.findById(id);
            if (existingStall.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "档口不存在 (ID: " + id + ")");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            Stall stall = existingStall.get();

            // 更新基本信息
            if (requestBody.containsKey("name")) {
                stall.setName((String) requestBody.get("name"));
            }
            if (requestBody.containsKey("cuisineType")) {
                stall.setCuisineType((String) requestBody.get("cuisineType"));
            }
            if (requestBody.containsKey("halalInfo")) {
                stall.setHalalInfo((String) requestBody.get("halalInfo"));
            }
            if (requestBody.containsKey("contact")) {
                stall.setContact((String) requestBody.get("contact"));
            }
            if (requestBody.containsKey("imageUrl")) {
                stall.setImageUrl((String) requestBody.get("imageUrl"));
            }

            // 更新坐标信息
            if (requestBody.containsKey("latitude")) {
                Object lat = requestBody.get("latitude");
                stall.setLatitude(lat != null ? Double.parseDouble(lat.toString()) : null);
            }
            if (requestBody.containsKey("longitude")) {
                Object lon = requestBody.get("longitude");
                stall.setLongitude(lon != null ? Double.parseDouble(lon.toString()) : null);
            }

            // 更新食堂关联
            if (requestBody.containsKey("cafeteriaId")) {
                Long cafeteriaId = requestBody.get("cafeteriaId") != null
                    ? Long.parseLong(requestBody.get("cafeteriaId").toString())
                    : null;

                if (cafeteriaId != null) {
                    Optional<Cafeteria> cafeteria = cafeteriaService.findById(cafeteriaId);
                    if (cafeteria.isPresent()) {
                        stall.setCafeteria(cafeteria.get());
                    } else {
                        Map<String, Object> error = new HashMap<>();
                        error.put("success", false);
                        error.put("message", "食堂不存在 (ID: " + cafeteriaId + ")");
                        return ResponseEntity.badRequest().body(error);
                    }
                } else {
                    stall.setCafeteria(null);
                }
            }

            Stall updatedStall = stallService.save(stall);

            // 使用辅助方法构建包含cafeteria信息的响应
            Map<String, Object> stallData = buildStallResponse(updatedStall);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "档口更新成功");
            response.put("stall", stallData);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("更新档口失败: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error("更新档口时发生异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 删除档口（管理员专用）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "删除档口 [管理员]", description = "删除指定ID的档口（级联删除关联的评价和图片）（需要管理员权限）")
    public ResponseEntity<Map<String, Object>> deleteStall(
            @Parameter(description = "档口ID") @PathVariable Long id) {
        try {
            log.info("管理员删除档口: ID={}", id);

            // 检查档口是否存在
            Optional<Stall> existingStall = stallService.findById(id);
            if (existingStall.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "档口不存在 (ID: " + id + ")");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // 删除档口
            stallService.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "档口删除成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("删除档口时发生异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 批量删除档口（管理员专用）
     */
    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "批量删除档口 [管理员]", description = "批量删除多个档口（需要管理员权限）")
    public ResponseEntity<Map<String, Object>> batchDeleteStalls(
            @RequestBody Map<String, List<Long>> requestBody) {
        try {
            List<Long> ids = requestBody.get("ids");
            log.info("管理员批量删除档口: {}", ids);

            int successCount = 0;
            List<String> errors = new ArrayList<>();

            for (Long id : ids) {
                try {
                    stallService.deleteById(id);
                    successCount++;
                } catch (Exception e) {
                    errors.add("删除档口 " + id + " 失败: " + e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", errors.isEmpty());
            response.put("message", String.format("成功删除 %d 个档口", successCount));
            response.put("successCount", successCount);
            response.put("totalCount", ids.size());
            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("批量删除档口时发生异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 获取档口列表（管理员视图，含详细信息和分页）
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "获取档口列表 [管理员]", description = "分页查询所有档口（管理员视图，需要管理员权限）")
    public ResponseEntity<Map<String, Object>> getStallListForAdmin(
            @Parameter(description = "页码(从0开始)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "食堂ID(可选)") @RequestParam(required = false) Long cafeteriaId,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "排序方向(ASC/DESC)") @RequestParam(defaultValue = "ASC") String sortDirection) {
        try {
            log.info("管理员查询档口列表: page={}, size={}, cafeteriaId={}, sortBy={}, sortDirection={}",
                    page, size, cafeteriaId, sortBy, sortDirection);

            List<Stall> stalls;

            if (cafeteriaId != null) {
                // 按食堂ID筛选
                stalls = stallService.findByCafeteriaId(cafeteriaId);
            } else {
                // 获取所有档口
                stalls = stallService.findAll();
            }

            // 排序处理
            if (sortDirection.equalsIgnoreCase("DESC")) {
                stalls.sort((s1, s2) -> {
                    if ("name".equals(sortBy)) {
                        return s2.getName().compareTo(s1.getName());
                    } else if ("cuisineType".equals(sortBy)) {
                        return s2.getCuisineType().compareTo(s1.getCuisineType());
                    } else if ("averageRating".equals(sortBy)) {
                        return Double.compare(s2.getAverageRating(), s1.getAverageRating());
                    } else {
                        return s2.getId().compareTo(s1.getId());
                    }
                });
            } else {
                stalls.sort((s1, s2) -> {
                    if ("name".equals(sortBy)) {
                        return s1.getName().compareTo(s2.getName());
                    } else if ("cuisineType".equals(sortBy)) {
                        return s1.getCuisineType().compareTo(s2.getCuisineType());
                    } else if ("averageRating".equals(sortBy)) {
                        return Double.compare(s1.getAverageRating(), s2.getAverageRating());
                    } else {
                        return s1.getId().compareTo(s2.getId());
                    }
                });
            }

            // 分页处理
            int start = page * size;
            int end = Math.min(start + size, stalls.size());
            List<Stall> pagedStalls = stalls.subList(start, end);

            // 使用buildStallResponse构建包含cafeteria信息的响应列表
            List<Map<String, Object>> stallDataList = pagedStalls.stream()
                .map(this::buildStallResponse)
                .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stalls", stallDataList);
            response.put("totalItems", stalls.size());
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("totalPages", (int) Math.ceil((double) stalls.size() / size));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取档口列表时发生异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    // ============ 辅助方法 ============

    /**
     * 构建包含cafeteria信息的stall响应对象
     * 解决@JsonBackReference导致的cafeteria字段不被序列化的问题
     */
    private Map<String, Object> buildStallResponse(Stall stall) {
        Map<String, Object> stallData = new HashMap<>();
        stallData.put("id", stall.getId());
        stallData.put("name", stall.getName());
        stallData.put("cuisineType", stall.getCuisineType());
        stallData.put("cuisine", stall.getCuisineType()); // 前端使用cuisine字段
        stallData.put("halalInfo", stall.getHalalInfo());
        stallData.put("halal", stall.getHalalInfo() != null && !stall.getHalalInfo().isEmpty());
        stallData.put("contact", stall.getContact());
        stallData.put("imageUrl", stall.getImageUrl());
        stallData.put("latitude", stall.getLatitude());
        stallData.put("longitude", stall.getLongitude());
        stallData.put("averageRating", stall.getAverageRating());
        stallData.put("reviewCount", stall.getReviewCount());
        stallData.put("averagePrice", stall.getAveragePrice());
        stallData.put("createdAt", stall.getCreatedAt());
        stallData.put("updatedAt", stall.getUpdatedAt());

        // 手动添加cafeteria信息（避免@JsonBackReference导致的序列化问题）
        if (stall.getCafeteria() != null) {
            Map<String, Object> cafeteriaData = new HashMap<>();
            cafeteriaData.put("id", stall.getCafeteria().getId());
            cafeteriaData.put("name", stall.getCafeteria().getName());
            cafeteriaData.put("location", stall.getCafeteria().getLocation());
            cafeteriaData.put("latitude", stall.getCafeteria().getLatitude());
            cafeteriaData.put("longitude", stall.getCafeteria().getLongitude());
            stallData.put("cafeteria", cafeteriaData);
            stallData.put("cafeteriaName", stall.getCafeteria().getName()); // 前端使用cafeteriaName字段
            stallData.put("cafeteriaId", stall.getCafeteria().getId());
        } else {
            stallData.put("cafeteria", null);
            stallData.put("cafeteriaName", null);
            stallData.put("cafeteriaId", null);
        }

        return stallData;
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
