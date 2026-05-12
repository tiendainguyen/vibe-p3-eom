package com.example.eom.controller;

import com.example.eom.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks")
@RequiredArgsConstructor
@Tag(name = "Webhooks", description = "Stripe webhook receiver")
public class StripeWebhookController {

    private final PaymentService paymentService;

    @PostMapping("/stripe")
    @Operation(summary = "Receive Stripe webhook events (public — verified by signature)")
    @ApiResponse(responseCode = "200", description = "Event processed")
    @ApiResponse(responseCode = "400", description = "Invalid signature or payload")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        paymentService.handleWebhookEvent(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}
