package com.nushungry.userservice.controller;

import com.nushungry.userservice.dto.DashboardStatsDTO;
import com.nushungry.userservice.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "管理员仪表板", description = "管理员仪表板统计API")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ROLE_ADMIN')")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "获取仪表板统计数据", description = "获取管理员仪表板所需的完整统计数据")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        log.info("管理员请求仪表板统计数据");
        DashboardStatsDTO stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/users")
    @Operation(summary = "获取用户统计卡片", description = "获取用户相关的统计卡片数据")
    public ResponseEntity<DashboardStatsDTO.StatsCards> getUserStats() {
        log.info("管理员请求用户统计卡片");
        DashboardStatsDTO.StatsCards stats = dashboardService.getStatsCards();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/system")
    @Operation(summary = "获取系统概览", description = "获取系统运行状态概览")
    public ResponseEntity<DashboardStatsDTO.SystemOverview> getSystemOverview() {
        log.info("管理员请求系统概览");
        DashboardStatsDTO.SystemOverview overview = dashboardService.getSystemOverview();
        return ResponseEntity.ok(overview);
    }

    @GetMapping("/user-growth")
    @Operation(summary = "获取用户增长数据", description = "根据日期范围获取用户增长统计数据")
    public ResponseEntity<List<DashboardStatsDTO.UserGrowthData>> getUserGrowth(
            @Parameter(description = "开始日期 (格式: yyyy-MM-dd)", example = "2025-11-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "结束日期 (格式: yyyy-MM-dd)", example = "2025-11-13")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        log.info("管理员请求用户增长数据: {} 到 {}", startDate, endDate);
        List<DashboardStatsDTO.UserGrowthData> growthData =
                dashboardService.getUserGrowthData(startDate, endDate);
        return ResponseEntity.ok(growthData);
    }
}
