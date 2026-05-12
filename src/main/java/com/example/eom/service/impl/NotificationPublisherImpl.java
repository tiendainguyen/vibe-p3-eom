package com.example.eom.service.impl;

import com.example.eom.service.NotificationPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationPublisherImpl implements NotificationPublisher {

    @Override
    public void publishOrderConfirmed(Long orderId) {
        // T-070 will replace this with RabbitMQ publishing
        log.info("Order confirmed (notification stub): orderId={}", orderId);
    }
}
