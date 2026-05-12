package com.example.eom.service.impl;

import com.example.eom.dto.user.UserResponse;
import com.example.eom.repository.UserRepository;
import com.example.eom.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(u -> new UserResponse(u.getId(), u.getEmail(), u.getRole().name(), u.getCreatedAt()))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }
}
