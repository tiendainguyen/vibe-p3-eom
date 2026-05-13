package com.example.eom.service.impl;

import com.example.eom.domain.User;
import com.example.eom.dto.user.AdminUserResponse;
import com.example.eom.dto.user.UserResponse;
import com.example.eom.exception.SelfDeactivationException;
import com.example.eom.repository.UserRepository;
import com.example.eom.service.UserService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final Set<String> SORTABLE_FIELDS = Set.of("id", "email", "role", "active", "createdAt");

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Long userId) {
        return userRepository.findById(userId)
                .map(u -> new UserResponse(u.getId(), u.getEmail(), u.getRole().name(), u.getCreatedAt()))
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdminUserResponse> listAll(Pageable pageable) {
        return userRepository.findAll(sanitizeSort(pageable)).map(this::toAdminResponse);
    }

    @Override
    @Transactional
    public AdminUserResponse activate(Long userId) {
        User user = findOrThrow(userId);
        user.setActive(true);
        return toAdminResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public AdminUserResponse deactivate(Long adminId, Long userId) {
        if (adminId.equals(userId)) {
            throw new SelfDeactivationException();
        }
        User user = findOrThrow(userId);
        user.setActive(false);
        return toAdminResponse(userRepository.save(user));
    }

    private Pageable sanitizeSort(Pageable pageable) {
        boolean valid = pageable.getSort().isUnsorted() ||
                pageable.getSort().stream().allMatch(o -> SORTABLE_FIELDS.contains(o.getProperty()));
        return valid ? pageable : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by("id"));
    }

    private User findOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));
    }

    private AdminUserResponse toAdminResponse(User u) {
        return new AdminUserResponse(u.getId(), u.getEmail(), u.getRole(), u.isActive(), u.getCreatedAt());
    }
}
