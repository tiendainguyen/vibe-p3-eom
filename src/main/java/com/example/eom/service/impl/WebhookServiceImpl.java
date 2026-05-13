package com.example.eom.service.impl;

import com.example.eom.domain.WebhookSubscription;
import com.example.eom.dto.webhook.CreateWebhookRequest;
import com.example.eom.dto.webhook.WebhookPayload;
import com.example.eom.dto.webhook.WebhookResponse;
import com.example.eom.repository.WebhookSubscriptionRepository;
import com.example.eom.service.WebhookService;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private static final Set<String> VALID_EVENT_TYPES = Set.of(
            EVENT_ORDER_PAID, EVENT_ORDER_SHIPPED, EVENT_ORDER_DELIVERED);

    private final WebhookSubscriptionRepository repository;
    private final RestTemplate webhookRestTemplate;

    public WebhookServiceImpl(WebhookSubscriptionRepository repository,
                               RestTemplate webhookRestTemplate) {
        this.repository = repository;
        this.webhookRestTemplate = webhookRestTemplate;
    }

    @Override
    @Transactional
    public WebhookResponse create(CreateWebhookRequest request) {
        for (String type : request.eventTypes()) {
            if (!VALID_EVENT_TYPES.contains(type)) {
                throw new IllegalArgumentException("Unknown event type: " + type);
            }
        }

        WebhookSubscription subscription = WebhookSubscription.builder()
                .url(request.url())
                .eventTypes(request.eventTypes())
                .active(true)
                .build();

        return toResponse(repository.save(subscription));
    }

    @Override
    @Transactional(readOnly = true)
    public List<WebhookResponse> listAll() {
        return repository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        WebhookSubscription subscription = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Webhook subscription not found: " + id));
        subscription.setActive(false);
        repository.save(subscription);
    }

    @Override
    @Async
    public void dispatch(String eventType, Long orderId) {
        List<WebhookSubscription> subscribers = repository.findActiveByEventType(eventType);
        if (subscribers.isEmpty()) {
            return;
        }

        WebhookPayload payload = new WebhookPayload(eventType, orderId, Instant.now());
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<WebhookPayload> entity = new HttpEntity<>(payload, headers);

        for (WebhookSubscription subscriber : subscribers) {
            try {
                webhookRestTemplate.postForEntity(subscriber.getUrl(), entity, Void.class);
                log.info("Webhook dispatched: eventType={}, orderId={}, url={}", eventType, orderId, subscriber.getUrl());
            } catch (Exception e) {
                log.error("Webhook dispatch failed: eventType={}, orderId={}, url={}, error={}",
                        eventType, orderId, subscriber.getUrl(), e.getMessage());
            }
        }
    }

    private WebhookResponse toResponse(WebhookSubscription s) {
        return new WebhookResponse(s.getId(), s.getUrl(), s.getEventTypes(), s.isActive(), s.getCreatedAt());
    }
}
