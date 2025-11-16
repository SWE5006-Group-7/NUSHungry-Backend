package com.nushungry.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token验证响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenVerifyResponse {

    private boolean valid;
    private String username;
    private String role;
    private String reason;
}
