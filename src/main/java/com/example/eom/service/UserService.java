package com.example.eom.service;

import com.example.eom.dto.user.UserResponse;

public interface UserService {

    UserResponse getCurrentUser(Long userId);
}