package com.example.eom.service;

import com.example.eom.dto.user.AdminUserResponse;
import com.example.eom.dto.user.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponse getCurrentUser(Long userId);

    Page<AdminUserResponse> listAll(Pageable pageable);

    AdminUserResponse activate(Long userId);

    AdminUserResponse deactivate(Long adminId, Long userId);
}