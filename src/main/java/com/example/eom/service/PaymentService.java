package com.example.eom.service;

import com.example.eom.dto.payment.CreatePaymentIntentRequest;
import com.example.eom.dto.payment.PaymentIntentResponse;

public interface PaymentService {

    PaymentIntentResponse createIntent(Long userId, CreatePaymentIntentRequest request);

    void handleWebhookEvent(String payload, String sigHeader);
}
