package com.nushungry.controller;

import com.nushungry.dto.DashboardStatsDTO;
import com.nushungry.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
@Tag(name = "管理员仪表板", description = "管理员仪表板统计API")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @Operation(summary = "获取仪表板统计数据", description = "获取管理员仪表板所需的统计数据")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats() {
        DashboardStatsDTO stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/users")
    @Operation(summary = "获取用户统计", description = "获取用户相关的统计数据")
    public ResponseEntity<DashboardStatsDTO.StatsCards> getUserStats() {
        DashboardStatsDTO stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats.getStatsCards());
    }

    @GetMapping("/stats/system")
    @Operation(summary = "获取系统概览", description = "获取系统运行状态概览")
    public ResponseEntity<DashboardStatsDTO.SystemOverview> getSystemOverview() {
        DashboardStatsDTO stats = dashboardService.getDashboardStats();
        return ResponseEntity.ok(stats.getSystemOverview());
    }
}
