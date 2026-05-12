package com.example.eom.service;

public interface NotificationPublisher {

    void publishOrderConfirmed(Long orderId);
}
