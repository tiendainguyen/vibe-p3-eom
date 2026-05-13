package com.example.eom.service;

import com.example.eom.domain.enums.OrderStatus;
import com.example.eom.dto.order.OrderResponse;
import com.example.eom.dto.payment.CreatePaymentIntentRequest;
import com.example.eom.dto.payment.PaymentIntentResponse;
import com.example.eom.service.gateway.StripeGateway;
import com.example.eom.service.impl.NotificationPublisherImpl;
import com.example.eom.service.impl.PaymentServiceImpl;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.PaymentIntent;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock StripeGateway stripeGateway;
    @Mock OrderService orderService;
    @Mock NotificationPublisher notificationPublisher;
    @Mock WebhookService webhookService;

    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 100L;
    private static final String PI_ID = "pi_test123";
    private static final String CLIENT_SECRET = "pi_test123_secret_abc";

    private PaymentServiceImpl paymentService() {
        return new PaymentServiceImpl(stripeGateway, orderService, notificationPublisher,
                webhookService, "whsec_dummy");
    }

    private OrderResponse pendingOrder() {
        return new OrderResponse(ORDER_ID, USER_ID, OrderStatus.PENDING, List.of(),
                new BigDecimal("179.98"), null, Instant.now(), Instant.now());
    }

    // ── createIntent ──────────────────────────────────────────────────────────

    @Test
    void createIntent_happyPath_returnsPaymentIntentResponse() throws Exception {
        given(orderService.getById(USER_ID, ORDER_ID)).willReturn(pendingOrder());

        PaymentIntent mockIntent = new PaymentIntent();
        ReflectionTestUtils.setField(mockIntent, "id", PI_ID);
        ReflectionTestUtils.setField(mockIntent, "clientSecret", CLIENT_SECRET);
        given(stripeGateway.createPaymentIntent(17998L, "usd", "100")).willReturn(mockIntent);
        willDoNothing().given(orderService).linkPaymentIntent(ORDER_ID, PI_ID);

        PaymentIntentResponse response = paymentService().createIntent(USER_ID,
                new CreatePaymentIntentRequest(ORDER_ID));

        assertThat(response.paymentIntentId()).isEqualTo(PI_ID);
        assertThat(response.clientSecret()).isEqualTo(CLIENT_SECRET);
        assertThat(response.orderId()).isEqualTo(ORDER_ID);
        assertThat(response.amount()).isEqualTo(17998L);
        assertThat(response.currency()).isEqualTo("usd");
        verify(orderService).linkPaymentIntent(ORDER_ID, PI_ID);
    }

    @Test
    void createIntent_orderNotFound_throwsEntityNotFound() {
        given(orderService.getById(USER_ID, ORDER_ID))
                .willThrow(new EntityNotFoundException("Order not found: " + ORDER_ID));

        assertThatThrownBy(() -> paymentService().createIntent(USER_ID,
                new CreatePaymentIntentRequest(ORDER_ID)))
                .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    void createIntent_wrongUser_throwsAccessDenied() {
        given(orderService.getById(USER_ID, ORDER_ID))
                .willThrow(new AccessDeniedException("Order does not belong to the current user"));

        assertThatThrownBy(() -> paymentService().createIntent(USER_ID,
                new CreatePaymentIntentRequest(ORDER_ID)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void createIntent_stripeFailure_throwsIllegalState() throws Exception {
        given(orderService.getById(USER_ID, ORDER_ID)).willReturn(pendingOrder());
        given(stripeGateway.createPaymentIntent(anyLong(), anyString(), anyString()))
                .willThrow(new com.stripe.exception.ApiException("Stripe error", null, null, 500, null));

        assertThatThrownBy(() -> paymentService().createIntent(USER_ID,
                new CreatePaymentIntentRequest(ORDER_ID)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to create payment intent");
    }

    // ── handleWebhookEvent ────────────────────────────────────────────────────

    @Test
    void handleWebhookEvent_invalidSignature_throwsIllegalArgument() throws Exception {
        given(stripeGateway.constructEvent(anyString(), anyString(), anyString()))
                .willThrow(new SignatureVerificationException("Bad sig", "header"));

        assertThatThrownBy(() -> paymentService().handleWebhookEvent("payload", "bad-sig"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid webhook signature");
    }
}
