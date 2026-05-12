package com.example.eom.service;

import com.example.eom.dto.auth.AuthResponse;
import com.example.eom.dto.auth.LoginRequest;
import com.example.eom.dto.auth.RegisterRequest;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}