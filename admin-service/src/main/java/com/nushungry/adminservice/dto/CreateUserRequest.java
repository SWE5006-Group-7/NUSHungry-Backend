package com.nushungry.adminservice.dto;

import lombok.Data;

@Data
public class CreateUserRequest {
    private String username;
    private String email;
    private String password;
    private String role;
    private boolean enabled = true;
}