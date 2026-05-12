package com.example.eom.repository;

import com.example.eom.domain.Order;
import com.example.eom.domain.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    java.util.Optional<Order> findByStripePaymentIntentId(String stripePaymentIntentId);

    @Query("""
            SELECT o FROM Order o
            WHERE (:status IS NULL OR o.status = :status)
              AND (:userId IS NULL OR o.userId = :userId)
              AND (:from IS NULL OR o.createdAt >= :from)
              AND (:to IS NULL OR o.createdAt <= :to)
            """)
    Page<Order> findAllWithFilters(
            @Param("status") OrderStatus status,
            @Param("userId") Long userId,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
