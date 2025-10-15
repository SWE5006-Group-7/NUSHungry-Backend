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
    public ResponseEntity<Map<String, Object>> testAuth(Principal principal) {
        if (principal == null) {
            return ResponseEntity.ok(Map.of("message", "未认证"));
        }
        
        return ResponseEntity.ok(Map.of(
            "message", "认证成功",
            "username", principal.getName()
        ));
    }
}