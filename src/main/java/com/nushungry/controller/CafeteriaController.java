package com.nushungry.controller;

import com.nushungry.model.Cafeteria;
import com.nushungry.model.Stall;
import com.nushungry.model.CafeteriaDetailDTO;
import com.nushungry.service.CafeteriaService;
import com.nushungry.service.ImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
 * 食堂管理控制器
 * 提供公开查询接口和管理员专用的CRUD操作接口
 */
@Slf4j
@RestController
@RequestMapping("/api/cafeterias")
@RequiredArgsConstructor
@Tag(name = "Cafeteria Management", description = "食堂管理接口")
public class CafeteriaController {

    private final CafeteriaService cafeteriaService;
    private final ImageService imageService;

    // ============ 公开接口（无需认证） ============

    /**
     * 获取所有食堂列表（公开接口）
     */
    @GetMapping
    @Operation(summary = "获取所有食堂", description = "查询所有食堂的基本信息列表")
    public ResponseEntity<List<Cafeteria>> getAllCafeterias() {
        log.info("查询所有食堂列表");
        List<Cafeteria> cafeterias = cafeteriaService.findAll();
        return ResponseEntity.ok(cafeterias);
    }

    /**
     * 根据ID获取食堂详细信息（公开接口）
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取食堂详情", description = "根据ID查询食堂详细信息（含档口、图片、菜系标签）")
    public ResponseEntity<CafeteriaDetailDTO> getCafeteriaById(
            @Parameter(description = "食堂ID") @PathVariable Long id) {
        log.info("查询食堂详情: ID={}", id);

        return cafeteriaService.findById(id)
                .map(cafeteria -> {
                    List<Stall> stalls = cafeteriaService.findStallsByCafeteriaId(id);
                    CafeteriaDetailDTO dto = new CafeteriaDetailDTO(
                        cafeteria.getId(),
                        cafeteria.getName(),
                        cafeteria.getDescription(),
                        cafeteria.getLocation(),
                        cafeteria.getLatitude(),
                        cafeteria.getLongitude(),
                        cafeteria.getImageUrl(),
                        cafeteria.getTermTimeOpeningHours(),
                        cafeteria.getVacationOpeningHours(),
                        cafeteria.getNearestBusStop(),
                        cafeteria.getNearestCarpark(),
                        cafeteria.getHalalInfo(),
                        cafeteria.getSeatingCapacity()
                    );
                    dto.setStalls(stalls);
                    // 聚合菜系标签
                    dto.setCuisineTags(cafeteriaService.aggregateCuisineTags(id));
                    // 添加图片列表
                    dto.setImages(imageService.getCafeteriaImages(id));
                    return ResponseEntity.ok(dto);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 获取热门食堂列表（公开接口）
     */
    @GetMapping("/popular")
    @Operation(summary = "获取热门食堂", description = "查询热门食堂列表")
    public ResponseEntity<List<Cafeteria>> getPopularCafeterias() {
        log.info("查询热门食堂列表");
        List<Cafeteria> cafeterias = cafeteriaService.findPopularCafeterias();
        return ResponseEntity.ok(cafeterias);
    }

    // ============ 管理员专用接口（需要 ADMIN 权限） ============

    /**
     * 创建新食堂（管理员专用）
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "创建食堂 [管理员]", description = "创建新的食堂信息（需要管理员权限）")
    public ResponseEntity<Map<String, Object>> createCafeteria(
            @Valid @RequestBody Cafeteria cafeteria) {
        try {
            log.info("管理员创建新食堂: {}", cafeteria.getName());

            // 保存食堂信息
            Cafeteria savedCafeteria = cafeteriaService.save(cafeteria);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "食堂创建成功");
            response.put("cafeteria", savedCafeteria);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (IllegalArgumentException e) {
            log.error("创建食堂失败: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error("创建食堂时发生异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 更新食堂信息（管理员专用）
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "更新食堂信息 [管理员]", description = "更新指定ID的食堂信息（需要管理员权限）")
    public ResponseEntity<Map<String, Object>> updateCafeteria(
            @Parameter(description = "食堂ID") @PathVariable Long id,
            @Valid @RequestBody Cafeteria cafeteria) {
        try {
            log.info("管理员更新食堂: ID={}, 名称={}", id, cafeteria.getName());

            // 检查食堂是否存在
            Optional<Cafeteria> existingCafeteria = cafeteriaService.findById(id);
            if (existingCafeteria.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "食堂不存在 (ID: " + id + ")");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // 设置ID并保存
            cafeteria.setId(id);
            Cafeteria updatedCafeteria = cafeteriaService.save(cafeteria);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "食堂更新成功");
            response.put("cafeteria", updatedCafeteria);

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("更新食堂失败: {}", e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(error);

        } catch (Exception e) {
            log.error("更新食堂时发生异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 删除食堂（管理员专用）
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "删除食堂 [管理员]", description = "删除指定ID的食堂（级联删除关联的档口）（需要管理员权限）")
    public ResponseEntity<Map<String, Object>> deleteCafeteria(
            @Parameter(description = "食堂ID") @PathVariable Long id) {
        try {
            log.info("管理员删除食堂: ID={}", id);

            // 检查食堂是否存在
            Optional<Cafeteria> existingCafeteria = cafeteriaService.findById(id);
            if (existingCafeteria.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "食堂不存在 (ID: " + id + ")");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            // 删除食堂
            cafeteriaService.deleteById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "食堂删除成功");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("删除食堂时发生异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 修改食堂营业状态（管理员专用）
     */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "修改食堂营业状态 [管理员]", description = "临时关闭或开启食堂营业（需要管理员权限）")
    public ResponseEntity<Map<String, Object>> updateCafeteriaStatus(
            @Parameter(description = "食堂ID") @PathVariable Long id,
            @Parameter(description = "营业状态") @RequestBody Map<String, String> statusRequest) {
        try {
            String status = statusRequest.get("status"); // "OPEN" 或 "CLOSED"
            log.info("管理员修改食堂营业状态: ID={}, 状态={}", id, status);

            // 检查食堂是否存在
            Optional<Cafeteria> optionalCafeteria = cafeteriaService.findById(id);
            if (optionalCafeteria.isEmpty()) {
                Map<String, Object> error = new HashMap<>();
                error.put("success", false);
                error.put("message", "食堂不存在 (ID: " + id + ")");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            Cafeteria cafeteria = optionalCafeteria.get();

            // 目前通过修改营业时间字符串来标记状态
            // 后续可以在Cafeteria实体中添加专门的status字段
            if ("CLOSED".equalsIgnoreCase(status)) {
                cafeteria.setTermTimeOpeningHours("暂停营业");
                cafeteria.setVacationOpeningHours("暂停营业");
            } else if ("OPEN".equalsIgnoreCase(status)) {
                // 恢复为默认营业时间(这里需要根据实际业务逻辑调整)
                if (cafeteria.getTermTimeOpeningHours().equals("暂停营业")) {
                    cafeteria.setTermTimeOpeningHours("周一至周五: 7:00-21:00");
                }
                if (cafeteria.getVacationOpeningHours().equals("暂停营业")) {
                    cafeteria.setVacationOpeningHours("周一至周日: 8:00-20:00");
                }
            }

            cafeteriaService.save(cafeteria);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "营业状态修改成功");
            response.put("status", status);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("修改营业状态时发生异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 获取食堂列表（管理员视图，含详细信息和分页）
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    @Operation(summary = "获取食堂列表 [管理员]", description = "分页查询所有食堂（管理员视图，需要管理员权限）")
    public ResponseEntity<Map<String, Object>> getCafeteriaListForAdmin(
            @Parameter(description = "页码(从0开始)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "排序方向(ASC/DESC)") @RequestParam(defaultValue = "ASC") String sortDirection) {
        try {
            log.info("管理员查询食堂列表: page={}, size={}, sortBy={}, sortDirection={}",
                    page, size, sortBy, sortDirection);

            // 构建排序
            Sort sort = sortDirection.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

            // 构建分页
            Pageable pageable = PageRequest.of(page, size, sort);

            // 获取所有食堂
            List<Cafeteria> cafeterias = cafeteriaService.findAll();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("cafeterias", cafeterias);
            response.put("totalItems", cafeterias.size());
            response.put("currentPage", page);
            response.put("pageSize", size);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("获取食堂列表时发生异常", e);
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("message", "服务器内部错误: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    /**
     * 统一异常处理
     */
    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, String> handleRuntimeException(RuntimeException ex) {
        log.error("处理运行时异常", ex);
        Map<String, String> error = new HashMap<>();
        error.put("message", ex.getMessage());
        return error;
    }
}
