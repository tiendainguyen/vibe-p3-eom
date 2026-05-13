package com.example.eom.dto.webhook;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;

import java.util.Set;

@Schema(description = "Request to register a new webhook subscription")
public record CreateWebhookRequest(

        @NotBlank(message = "url is required")
        @URL(protocol = "https", message = "URL must use HTTPS")
        @Schema(description = "HTTPS endpoint that will receive event POSTs",
                example = "https://my-storefront.example.com/webhooks/orders")
        String url,

        @NotEmpty(message = "At least one event type is required")
        @Schema(description = "Event types to subscribe to",
                example = "[\"order.paid\", \"order.shipped\"]",
                allowableValues = {"order.paid", "order.shipped", "order.delivered"})
        Set<String> eventTypes
) {}
