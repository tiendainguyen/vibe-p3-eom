package com.example.eom.dto.notification;

public record OrderShippedEvent(
        Long orderId,
        Long userId,
        String userEmail,
        String trackingInfo
) {}
