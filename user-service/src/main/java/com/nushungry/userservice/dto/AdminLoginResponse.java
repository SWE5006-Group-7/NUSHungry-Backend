package com.nushungry.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员登录响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminLoginResponse {

    private String token;
    private String tokenType;
    private Long expiresIn;
    private String username;
    private String email;
    private String role;
    private boolean isAdmin;
}
