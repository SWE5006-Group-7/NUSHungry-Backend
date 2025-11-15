package com.nushungry.cafeteriaservice.controller;

import com.nushungry.cafeteriaservice.dto.ApiResponse;
import com.nushungry.cafeteriaservice.model.Cafeteria;
import com.nushungry.cafeteriaservice.model.CafeteriaStatus;
import com.nushungry.cafeteriaservice.service.CafeteriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 管理员食堂管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/cafeterias")
@Tag(name = "管理员食堂管理", description = "管理员食堂管理相关接口")
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Authentication")
public class AdminCafeteriaController {

    private final CafeteriaService cafeteriaService;

    public AdminCafeteriaController(CafeteriaService cafeteriaService) {
        this.cafeteriaService = cafeteriaService;
    }

    /**
     * 修改食堂营业状态
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "修改食堂营业状态", description = "管理员修改指定食堂的营业状态")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateCafeteriaStatus(
            @Parameter(description = "食堂ID") @PathVariable Long id,
            @Parameter(description = "新状态") @RequestParam CafeteriaStatus status) {

        try {
            Cafeteria cafeteria = cafeteriaService.findById(id);
            if (cafeteria == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("食堂不存在"));
            }
            cafeteria.setStatus(status);
            Cafeteria saved = cafeteriaService.save(cafeteria);

            log.info("食堂状态更新成功: ID={}, 新状态={}", id, status);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", "食堂状态更新成功");
            responseData.put("cafeteria", saved);

            return ResponseEntity.ok(ApiResponse.success(responseData));

        } catch (Exception e) {
            log.error("更新食堂状态失败: ID={}, 状态={}", id, status, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新食堂状态失败: " + e.getMessage()));
        }
    }

    /**
     * 分页查询食堂列表
     */
    @GetMapping
    @Operation(summary = "分页查询食堂列表", description = "管理员分页查询食堂列表，支持排序")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllCafeterias(
            @Parameter(description = "页码（从0开始）") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "name") String sortBy,
            @Parameter(description = "排序方向") @RequestParam(defaultValue = "asc") String sortDirection) {

        try {
            Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ?
                    Sort.Direction.DESC : Sort.Direction.ASC;
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

            Page<Cafeteria> cafeteriaPage = cafeteriaService.findAll(pageable);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("content", cafeteriaPage.getContent());
            responseData.put("currentPage", cafeteriaPage.getNumber());
            responseData.put("totalItems", cafeteriaPage.getTotalElements());
            responseData.put("totalPages", cafeteriaPage.getTotalPages());
            responseData.put("pageSize", cafeteriaPage.getSize());

            return ResponseEntity.ok(ApiResponse.success(responseData));

        } catch (Exception e) {
            log.error("查询食堂列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("查询食堂列表失败: " + e.getMessage()));
        }
    }

    /**
     * 获取食堂详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取食堂详情", description = "管理员获取指定食堂的详细信息")
    public ResponseEntity<ApiResponse<Cafeteria>> getCafeteriaById(
            @Parameter(description = "食堂ID") @PathVariable Long id) {

        try {
            Cafeteria cafeteria = cafeteriaService.findById(id);
            if (cafeteria == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("食堂不存在"));
            }

            return ResponseEntity.ok(ApiResponse.success(cafeteria));

        } catch (Exception e) {
            log.error("获取食堂详情失败: ID={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("获取食堂详情失败: " + e.getMessage()));
        }
    }

    /**
     * 创建食堂
     */
    @PostMapping
    @Operation(summary = "创建食堂", description = "管理员创建新的食堂")
    public ResponseEntity<ApiResponse<Cafeteria>> create(@RequestBody Cafeteria cafeteria) {
        try {
            // 设置默认状态为营业中
            if (cafeteria.getStatus() == null) {
                cafeteria.setStatus(CafeteriaStatus.OPEN);
            }

            Cafeteria saved = cafeteriaService.save(cafeteria);
            log.info("食堂创建成功: ID={}, 名称={}", saved.getId(), saved.getName());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(saved));

        } catch (Exception e) {
            log.error("创建食堂失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("创建食堂失败: " + e.getMessage()));
        }
    }

    /**
     * 更新食堂信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新食堂信息", description = "管理员更新食堂的基本信息")
    public ResponseEntity<ApiResponse<Cafeteria>> update(
            @Parameter(description = "食堂ID") @PathVariable Long id,
            @RequestBody Cafeteria cafeteria) {

        try {
            if (cafeteriaService.findById(id) == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("食堂不存在"));
            }

            cafeteria.setId(id);
            Cafeteria saved = cafeteriaService.save(cafeteria);
            log.info("食堂信息更新成功: ID={}", id);

            return ResponseEntity.ok(ApiResponse.success(saved));

        } catch (Exception e) {
            log.error("更新食堂信息失败: ID={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("更新食堂信息失败: " + e.getMessage()));
        }
    }

    /**
     * 删除食堂
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除食堂", description = "管理员删除指定食堂")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Long id) {
        try {
            if (cafeteriaService.findById(id) == null) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("食堂不存在"));
            }

            cafeteriaService.deleteById(id);
            log.info("食堂删除成功: ID={}", id);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("message", "食堂删除成功");

            return ResponseEntity.ok(ApiResponse.success(responseData));

        } catch (Exception e) {
            log.error("删除食堂失败: ID={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("删除食堂失败: " + e.getMessage()));
        }
    }
}


