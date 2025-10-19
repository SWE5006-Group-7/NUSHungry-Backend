package com.nushungry.adminservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UpdateUserRequest {
    private String username;
    private String email;
    private String role;
    private Boolean enabled;
}