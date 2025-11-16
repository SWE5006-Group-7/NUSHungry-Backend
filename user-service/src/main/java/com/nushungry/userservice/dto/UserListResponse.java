package com.nushungry.userservice.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class UserListResponse {
    private List<UserDTO> users;
    private int currentPage;
    private long totalItems;
    private int totalPages;
    private int pageSize;
}
