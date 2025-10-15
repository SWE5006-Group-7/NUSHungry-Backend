package com.nushungry.adminservice.dto;

import lombok.Data;
import java.util.List;

@Data
public class UserListResponse {
    private List<UserDTO> users;
    private int currentPage;
    private long totalItems;
    private int totalPages;
    private int pageSize;
}