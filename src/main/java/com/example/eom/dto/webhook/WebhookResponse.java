package com.example.eom.dto.webhook;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;

@Schema(description = "Webhook subscription details")
public record WebhookResponse(

        @Schema(description = "Subscription ID", example = "1")
        Long id,

        @Schema(description = "Target HTTPS endpoint", example = "https://my-storefront.example.com/webhooks/orders")
        String url,

        @Schema(description = "Subscribed event types", example = "[\"order.paid\"]")
        Set<String> eventTypes,

        @Schema(description = "Whether this subscription is active", example = "true")
        boolean active,

        @Schema(description = "Subscription creation timestamp")
        Instant createdAt
) {}
