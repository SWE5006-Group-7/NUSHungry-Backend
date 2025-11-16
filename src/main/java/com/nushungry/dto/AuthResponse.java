package com.nushungry.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String refreshToken;
    private Long expiresIn;
    private Long id;
    private String username;
    private String email;
    private String avatarUrl;
    private String role;

    // 保持向后兼容的构造函数(不含 refresh token 和 role)
    public AuthResponse(String token, Long id, String username, String email, String avatarUrl) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
    }

    // 构造函数(包含 role，不含 refresh token)
    public AuthResponse(String token, Long id, String username, String email, String avatarUrl, String role) {
        this.token = token;
        this.id = id;
        this.username = username;
        this.email = email;
        this.avatarUrl = avatarUrl;
        this.role = role;
    }
}
