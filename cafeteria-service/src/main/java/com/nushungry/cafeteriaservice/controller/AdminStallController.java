package com.nushungry.cafeteriaservice.controller;

import com.nushungry.cafeteriaservice.model.Cafeteria;
import com.nushungry.cafeteriaservice.model.Stall;
import com.nushungry.cafeteriaservice.service.CafeteriaService;
import com.nushungry.cafeteriaservice.service.StallService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 管理员摊位管理控制器
 * 提供摊位管理相关的API端点(CRUD操作)
 */
@Slf4j
@RestController
@RequestMapping("/api/stalls/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Stall Management", description = "管理员摊位管理接口")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminStallController {

    private final StallService stallService;
    private final CafeteriaService cafeteriaService;

    /**
     * 创建新摊位
     */
    @PostMapping
    @Operation(summary = "创建摊位", description = "创建新的摊位信息")
    public ResponseEntity<Map<String, Object>> createStall(
            @RequestBody Map<String, Object> requestBody) {
        try {
            log.info("管理员创建新摊位: {}", requestBody.get("name"));

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
                Optional<Cafeteria> cafeteria = cafeteriaService.findByIdOptional(cafeteriaId);
                if (cafeteria.isPresent()) {
                    stall.setCafeteria(cafeteria.get());
                } else {
                    Map<String, Object> error = new HashMap<>();
                    error.put("success", false);
                    error.put("message", "食堂不存在 (ID: " + cafeteriaId + ")");
                    return ResponseEntity.badRequest().body(error);
                }
            }

            // 保存摊位信息
            Stall savedStall = stallService.save(stall);

            // 使用辅助方法构建包含cafeteria信息的响应
            Map<String, Object> stallData = buildStallResponse(savedStall);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "摊位创建成功");
            response.put("stall", stallData);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("创建摊位失败: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error("创建摊位时发生异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 更新摊位信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新摊位信息", description = "更新指定ID的摊位信息")
    public ResponseEntity<Map<String, Object>> updateStall(
            @Parameter(description = "摊位ID") @PathVariable Long id,
            @RequestBody Map<String, Object> requestBody) {
        try {
            log.info("管理员更新摊位: ID={}, 名称={}", id, requestBody.get("name"));

            // 检查摊位是否存在
            Optional<Stall> existingStall = stallService.findById(id);
            if (existingStall.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "摊位不存在 (ID: " + id + ")");
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
                    Optional<Cafeteria> cafeteria = cafeteriaService.findByIdOptional(cafeteriaId);
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
            response.put("message", "摊位更新成功");
            response.put("stall", stallData);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("更新摊位失败: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error("更新摊位时发生异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 删除摊位
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除摊位", description = "删除指定ID的摊位(级联删除关联的评价和图片)")
    public ResponseEntity<Map<String, Object>> deleteStall(
            @Parameter(description = "摊位ID") @PathVariable Long id) {
        try {
            log.info("管理员删除摊位: ID={}", id);

            // 检查摊位是否存在
            Optional<Stall> existingStall = stallService.findById(id);
            if (existingStall.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "摊位不存在 (ID: " + id + ")");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // 删除摊位
            stallService.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "摊位删除成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("删除摊位时发生异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 获取摊位列表(管理员视图)
     */
    @GetMapping
    @Operation(summary = "获取摊位列表", description = "分页查询所有摊位(管理员视图)")
    public ResponseEntity<Map<String, Object>> getStallList(
            @Parameter(description = "页码(从0开始)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "食堂ID(可选)") @RequestParam(required = false) Long cafeteriaId,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "排序方向(ASC/DESC)") @RequestParam(defaultValue = "ASC") String sortDirection) {
        try {
            List<Stall> stalls;

            if (cafeteriaId != null) {
                // 按食堂ID筛选
                stalls = stallService.findByCafeteriaId(cafeteriaId);
            } else {
                // 获取所有摊位
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
                .collect(java.util.stream.Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stalls", stallDataList);
            response.put("totalItems", stalls.size());
            response.put("currentPage", page);
            response.put("pageSize", size);
            response.put("totalPages", (int) Math.ceil((double) stalls.size() / size));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取摊位列表时发生异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 获取摊位详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取摊位详情", description = "获取指定ID的摊位详细信息")
    public ResponseEntity<Map<String, Object>> getStallDetail(
            @Parameter(description = "摊位ID") @PathVariable Long id) {
        try {
            Optional<Stall> stall = stallService.findById(id);

            if (stall.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "摊位不存在 (ID: " + id + ")");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // 使用辅助方法构建包含cafeteria信息的响应
            Map<String, Object> stallData = buildStallResponse(stall.get());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stall", stallData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取摊位详情时发生异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 批量删除摊位
     */
    @DeleteMapping("/batch")
    @Operation(summary = "批量删除摊位", description = "批量删除多个摊位")
    public ResponseEntity<Map<String, Object>> batchDeleteStalls(
            @RequestBody Map<String, List<Long>> requestBody) {
        try {
            List<Long> ids = requestBody.get("ids");
            log.info("管理员批量删除摊位: {}", ids);

            int successCount = 0;
            List<String> errors = new ArrayList<>();

            for (Long id : ids) {
                try {
                    stallService.deleteById(id);
                    successCount++;
                } catch (Exception e) {
                    errors.add("删除摊位 " + id + " 失败: " + e.getMessage());
                }
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", errors.isEmpty());
            response.put("message", String.format("成功删除 %d 个摊位", successCount));
            response.put("successCount", successCount);
            response.put("totalCount", ids.size());
            if (!errors.isEmpty()) {
                response.put("errors", errors);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("批量删除摊位时发生异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 构建包含cafeteria信息的stall响应对象
     * 解决@JsonBackReference导致的cafeteria字段不被序列化的问题
     */
    private Map<String, Object> buildStallResponse(Stall stall) {
        Map<String, Object> stallData = new HashMap<>();
        stallData.put("id", stall.getId());
        stallData.put("name", stall.getName());
        stallData.put("cuisineType", stall.getCuisineType());
        stallData.put("halalInfo", stall.getHalalInfo());
        stallData.put("contact", stall.getContact());
        stallData.put("imageUrl", stall.getImageUrl());
        stallData.put("latitude", stall.getLatitude());
        stallData.put("longitude", stall.getLongitude());
        stallData.put("averageRating", stall.getAverageRating());
        stallData.put("reviewCount", stall.getReviewCount());
        stallData.put("createdAt", stall.getCreatedAt());
        stallData.put("updatedAt", stall.getUpdatedAt());

        // 手动添加cafeteria信息（避免@JsonBackReference导致的序列化问题）
        if (stall.getCafeteria() != null) {
            Map<String, Object> cafeteriaData = new HashMap<>();
            cafeteriaData.put("id", stall.getCafeteria().getId());
            cafeteriaData.put("name", stall.getCafeteria().getName());
            cafeteriaData.put("location", stall.getCafeteria().getLocation());
            stallData.put("cafeteria", cafeteriaData);
            stallData.put("cafeteriaId", stall.getCafeteria().getId());
        } else {
            stallData.put("cafeteria", null);
            stallData.put("cafeteriaId", null);
        }

        return stallData;
    }
}
