package com.example.eom.service;

public interface NotificationPublisher {

    void publishOrderConfirmed(Long orderId);

    void publishOrderShipped(Long orderId, String trackingInfo);
}
