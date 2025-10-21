package com.nushungry.adminservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
@Tag(name = "Admin Auth", description = "管理员认证测试接口")
public class AdminAuthController {

    @GetMapping("/test")
    @Operation(summary = "测试认证", description = "测试JWT认证是否正常工作")
    public ResponseEntity<Map<String, Object>> testAuth() {
        // 从 SecurityContext 获取认证信息
        org.springframework.security.core.Authentication authentication =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
            authentication.getPrincipal().equals("anonymousUser")) {
            return ResponseEntity.ok(Map.of("message", "未认证"));
        }

        String username = authentication.getName();
        return ResponseEntity.ok(Map.of(
            "message", "认证成功",
            "username", username
        ));
    }
}