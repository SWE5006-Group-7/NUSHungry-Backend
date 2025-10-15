package com.nushungry.adminservice.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {
    private String username;
    private String email;
    private String role;
    private Boolean enabled;
}