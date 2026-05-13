package com.example.eom.repository;

import com.example.eom.domain.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WebhookSubscriptionRepository extends JpaRepository<WebhookSubscription, Long> {

    @Query("SELECT s FROM WebhookSubscription s JOIN s.eventTypes e WHERE e = :eventType AND s.active = true")
    List<WebhookSubscription> findActiveByEventType(@Param("eventType") String eventType);
}
