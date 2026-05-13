package com.example.eom.dto.webhook;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "Outbound webhook payload sent to subscribers")
public record WebhookPayload(

        @Schema(description = "Event type that triggered this webhook", example = "order.paid")
        String eventType,

        @Schema(description = "ID of the affected order", example = "100")
        Long orderId,

        @Schema(description = "Timestamp when the event occurred")
        Instant timestamp
) {}
