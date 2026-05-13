package com.example.eom.dto.notification;

import java.math.BigDecimal;

public record OrderConfirmedEvent(
        Long orderId,
        Long userId,
        String userEmail,
        BigDecimal orderTotal,
        int itemCount
) {}
