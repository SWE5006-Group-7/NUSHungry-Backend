package com.nushungry.adminservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateUserRequest {
    private String username;
    private String email;
    private String password;
    private String role;
    private boolean enabled = true;
}