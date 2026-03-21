package com.ticketbooking.payment.gateway.impl;

import com.ticketbooking.payment.gateway.PaymentGateway;
import com.ticketbooking.payment.gateway.dto.PaymentLinkRequest;
import com.ticketbooking.payment.gateway.dto.PaymentLinkResponse;
import com.ticketbooking.payment.gateway.dto.PaymentVerificationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Stripe payment gateway implementation with simulated failure.
 *
 * This implementation ALWAYS returns failure to demonstrate error handling
 * and compensating transactions in the SAGA pattern.
 *
 * NO REAL STRIPE API CALLS ARE MADE.
 */
@Slf4j
@Component
public class StripePaymentGateway implements PaymentGateway {

    private static final String MOCK_ORDER_ID_PREFIX = "stripe_mock_";

    private final String baseUrl;
    private final int paymentLinkExpiryMinutes;

    public StripePaymentGateway(
            @Value("${server.base-url:http://localhost:8082}") String baseUrl,
            @Value("${payment.stripe.payment-link-expiry-minutes:15}") int paymentLinkExpiryMinutes
    ) {
        this.baseUrl = baseUrl;
        this.paymentLinkExpiryMinutes = paymentLinkExpiryMinutes;
        log.info("Stripe Payment Gateway initialized (SIMULATION MODE - always fails)");
    }

    @Override
    public PaymentLinkResponse createPaymentLink(PaymentLinkRequest request) {
        log.warn("Stripe payment initiated - WILL ALWAYS FAIL (simulated)");
        log.info("Order: {}, Amount: {} {}", request.getOrderId(), request.getAmount(), request.getCurrency());

        // Generate mock gateway order ID
        String mockOrderId = MOCK_ORDER_ID_PREFIX + UUID.randomUUID().toString().substring(0, 8);

        // Return mock payment URL that points to a failure page
        String mockPaymentUrl = baseUrl + "/api/payments/stripe-failure-page/" + mockOrderId;

        log.info("Stripe mock payment URL generated: {}", mockPaymentUrl);

        return PaymentLinkResponse.builder()
                .paymentUrl(mockPaymentUrl)
                .gatewayOrderId(mockOrderId)
                .expiresAt(LocalDateTime.now().plusMinutes(paymentLinkExpiryMinutes))
                .build();
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        log.warn("Stripe webhook signature verification called - returning false (simulation mode)");
        // In simulation mode, we don't receive real webhooks
        return false;
    }

    @Override
    public PaymentVerificationResult verifyPayment(String gatewayOrderId) {
        log.info("Verifying Stripe payment (simulation): {} - ALWAYS FAILS", gatewayOrderId);

        // Always return failure
        return PaymentVerificationResult.builder()
                .success(false)
                .transactionId(null)
                .amount(null)
                .status("declined")
                .failureReason("Stripe payment declined (simulated failure for testing)")
                .build();
    }
}
