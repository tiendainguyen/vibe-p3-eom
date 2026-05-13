package com.example.eom.service;

import com.example.eom.domain.WebhookSubscription;
import com.example.eom.dto.webhook.CreateWebhookRequest;
import com.example.eom.dto.webhook.WebhookPayload;
import com.example.eom.dto.webhook.WebhookResponse;
import com.example.eom.repository.WebhookSubscriptionRepository;
import com.example.eom.service.impl.WebhookServiceImpl;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebhookServiceImplTest {

    @Mock WebhookSubscriptionRepository repository;
    @Mock RestTemplate webhookRestTemplate;

    private WebhookServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WebhookServiceImpl(repository, webhookRestTemplate);
    }

    private WebhookSubscription subscription(boolean active) {
        WebhookSubscription s = new WebhookSubscription();
        s.setId(1L);
        s.setUrl("https://example.com/hook");
        s.setEventTypes(Set.of(WebhookService.EVENT_ORDER_PAID));
        s.setActive(active);
        s.setCreatedAt(Instant.now());
        return s;
    }

    // ── create ────────────────────────────────────────────────────────────────

    @Test
    void create_validRequest_savesAndReturnsResponse() {
        CreateWebhookRequest req = new CreateWebhookRequest(
                "https://example.com/hook", Set.of(WebhookService.EVENT_ORDER_PAID));
        WebhookSubscription saved = subscription(true);
        given(repository.save(any())).willReturn(saved);

        WebhookResponse response = service.create(req);

        assertThat(response.url()).isEqualTo("https://example.com/hook");
        assertThat(response.active()).isTrue();
        assertThat(response.eventTypes()).contains(WebhookService.EVENT_ORDER_PAID);
    }

    @Test
    void create_unknownEventType_throwsIllegalArgument() {
        CreateWebhookRequest req = new CreateWebhookRequest(
                "https://example.com/hook", Set.of("order.unknown"));

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown event type");
    }

    // ── delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_existingSubscription_setsActiveFalse() {
        WebhookSubscription sub = subscription(true);
        given(repository.findById(1L)).willReturn(Optional.of(sub));
        given(repository.save(any())).willAnswer(inv -> inv.getArgument(0));

        service.delete(1L);

        ArgumentCaptor<WebhookSubscription> captor = ArgumentCaptor.forClass(WebhookSubscription.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().isActive()).isFalse();
    }

    @Test
    void delete_notFound_throwsEntityNotFound() {
        given(repository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L))
                .isInstanceOf(EntityNotFoundException.class);
    }

    // ── dispatch ──────────────────────────────────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void dispatch_activeSubscriber_postsPayload() {
        given(repository.findActiveByEventType(WebhookService.EVENT_ORDER_PAID))
                .willReturn(List.of(subscription(true)));
        given(webhookRestTemplate.postForEntity(any(String.class), any(), eq(Void.class)))
                .willReturn(ResponseEntity.ok().build());

        service.dispatch(WebhookService.EVENT_ORDER_PAID, 100L);

        ArgumentCaptor<HttpEntity<WebhookPayload>> captor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(webhookRestTemplate).postForEntity(
                eq("https://example.com/hook"), captor.capture(), eq(Void.class));

        WebhookPayload payload = captor.getValue().getBody();
        assertThat(payload).isNotNull();
        assertThat(payload.eventType()).isEqualTo(WebhookService.EVENT_ORDER_PAID);
        assertThat(payload.orderId()).isEqualTo(100L);
    }

    @Test
    void dispatch_noActiveSubscribers_noHttpCall() {
        given(repository.findActiveByEventType(WebhookService.EVENT_ORDER_PAID))
                .willReturn(List.of());

        service.dispatch(WebhookService.EVENT_ORDER_PAID, 100L);

        verify(webhookRestTemplate, never()).postForEntity(any(), any(), any());
    }

    @Test
    void dispatch_httpFailure_doesNotPropagate() {
        given(repository.findActiveByEventType(WebhookService.EVENT_ORDER_PAID))
                .willReturn(List.of(subscription(true)));
        given(webhookRestTemplate.postForEntity(any(String.class), any(), eq(Void.class)))
                .willThrow(new RuntimeException("Connection refused"));

        // must not throw
        service.dispatch(WebhookService.EVENT_ORDER_PAID, 100L);
    }
}
