package com.nushungry.adminservice.service;

import com.nushungry.adminservice.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@FeignClient(name = "user-service", url = "${user.service.url:http://localhost:8081}")
public interface UserServiceClient {
    
    @GetMapping("/api/admin/users")
    UserListResponse getUserList(@RequestParam("page") int page, 
                                @RequestParam("size") int size, 
                                @RequestParam("sortBy") String sortBy, 
                                @RequestParam("sortDirection") String sortDirection,
                                @RequestParam(value = "search", required = false) String search);
    
    @GetMapping("/api/admin/users/{id}")
    UserDTO getUserById(@PathVariable("id") Long id);
    
    @PostMapping("/api/admin/users")
    UserDTO createUser(@RequestBody CreateUserRequest request);
    
    @PutMapping("/api/admin/users/{id}")
    UserDTO updateUser(@PathVariable("id") Long id, @RequestBody UpdateUserRequest request);
    
    @DeleteMapping("/api/admin/users/{id}")
    void deleteUser(@PathVariable("id") Long id);
    
    @PatchMapping("/api/admin/users/{id}/status")
    UserDTO updateUserStatus(@PathVariable("id") Long id, @RequestBody boolean enabled);
    
    @PutMapping("/api/admin/users/{id}/role")
    UserDTO updateUserRole(@PathVariable("id") Long id, @RequestBody String role);
    
    @PostMapping("/api/admin/users/{id}/reset-password")
    void resetUserPassword(@PathVariable("id") Long id, @RequestBody String password);
    
    @PostMapping("/api/admin/users/batch")
    int batchOperation(@RequestBody BatchOperationRequest request);
}