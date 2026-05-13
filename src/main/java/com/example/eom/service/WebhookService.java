package com.example.eom.service;

import com.example.eom.dto.webhook.CreateWebhookRequest;
import com.example.eom.dto.webhook.WebhookResponse;

import java.util.List;

public interface WebhookService {

    String EVENT_ORDER_PAID      = "order.paid";
    String EVENT_ORDER_SHIPPED   = "order.shipped";
    String EVENT_ORDER_DELIVERED = "order.delivered";

    WebhookResponse create(CreateWebhookRequest request);

    List<WebhookResponse> listAll();

    void delete(Long id);

    void dispatch(String eventType, Long orderId);
}
