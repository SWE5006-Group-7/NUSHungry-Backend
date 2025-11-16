package com.nushungry.userservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private String type = "Bearer";
    private Long expiresIn;

    // 兼容原项目的字段结构
    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private String role;
}