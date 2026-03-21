package com.ticketbooking.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketbooking.common.event.PaymentCompletedEvent;
import com.ticketbooking.common.event.PaymentFailedEvent;
import com.ticketbooking.common.event.PaymentInitiatedEvent;
import com.ticketbooking.common.event.TicketReservedEvent;
import com.ticketbooking.common.exception.ResourceNotFoundException;
import com.ticketbooking.payment.exception.InvalidWebhookSignatureException;
import com.ticketbooking.payment.exception.PaymentGatewayException;
import com.ticketbooking.payment.gateway.PaymentGateway;
import com.ticketbooking.payment.gateway.PaymentGatewayFactory;
import com.ticketbooking.payment.gateway.dto.PaymentLinkRequest;
import com.ticketbooking.payment.gateway.dto.PaymentLinkResponse;
import com.ticketbooking.payment.gateway.dto.PaymentVerificationResult;
import com.ticketbooking.payment.dto.PaymentInitiateRequest;
import com.ticketbooking.payment.dto.PaymentInitiateResponse;
import com.ticketbooking.payment.model.Payment;
import com.ticketbooking.payment.model.PaymentProvider;
import com.ticketbooking.payment.model.PaymentStatus;
import com.ticketbooking.payment.producer.PaymentEventProducer;
import com.ticketbooking.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Payment Service - Processes payments using real payment gateways.
 *
 * Integrates with Razorpay (real) and Stripe (simulated failure) payment gateways.
 * Handles payment link generation and webhook callbacks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private static final String PAYMENT_METHOD_ONLINE = "ONLINE";
    private static final String SIMULATED_TRANSACTION_PREFIX = "SIM-";
    private static final String EVENT_TYPE_PAYMENT_INITIATED = "payment.initiated";

    private final PaymentRepository paymentRepository;
    private final PaymentEventProducer eventProducer;
    private final PaymentGatewayFactory gatewayFactory;
    private final ObjectMapper objectMapper;

    @Value("${server.base-url:http://localhost:8082}")
    private String baseUrl;

    /**
     * Process payment for a ticket reservation.
     * Creates payment link or immediately fails based on provider.
     */
    @Transactional
    public void processPayment(TicketReservedEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("TicketReservedEvent cannot be null");
        }
        if (event.getTicketId() == null || event.getUserId() == null || event.getTotalAmount() == null) {
            throw new IllegalArgumentException("Required event fields cannot be null");
        }

        PaymentProvider provider = PaymentProvider.fromString(event.getPaymentProvider());

        log.info("Processing payment for ticket: {}, amount: {}, provider: {}, correlationId: {}",
                event.getTicketId(), event.getTotalAmount(), provider, event.getCorrelationId());

        // Create payment record
        Payment payment = Payment.builder()
                .ticketId(event.getTicketId())
                .userId(event.getUserId())
                .amount(event.getTotalAmount())
                .status(PaymentStatus.PENDING)
                .paymentMethod(PAYMENT_METHOD_ONLINE)
                .paymentProvider(provider)
                .correlationId(event.getCorrelationId())
                .build();

        payment = paymentRepository.save(payment);

        // Handle payment based on provider
        switch (provider) {
            case STRIPE -> handleStripeSimulatedFailure(payment);
            case RAZORPAY -> handleRazorpayPayment(payment, event);
            case SIMULATED -> handleSimulatedPayment(payment);
        }
    }

    /**
     * Initiate payment synchronously - for smooth UI flow like BookMyShow.
     * Creates payment and returns payment URL immediately (no polling needed).
     * NOTE: Not @Transactional to avoid holding DB connection during external API calls.
     */
    public PaymentInitiateResponse initiatePaymentSync(PaymentInitiateRequest request) {
        log.info("Initiating payment synchronously for ticket: {}, provider: {}",
                request.getTicketId(), request.getPaymentProvider());

        PaymentProvider provider = PaymentProvider.fromString(request.getPaymentProvider());

        // Create payment record in separate transaction
        Payment payment = createPaymentRecordSync(request, provider);

        // Handle payment based on provider
        try {
            switch (provider) {
                case STRIPE -> {
                    payment.setStatus(PaymentStatus.FAILED);
                    payment.setFailureReason("Stripe payment declined (simulated failure for testing)");
                    payment.setPaymentUrl(baseUrl + "/api/payments/stripe-failure-page/" + payment.getId());
                    paymentRepository.save(payment);
                    publishPaymentFailed(payment);

                    return PaymentInitiateResponse.builder()
                            .paymentId(payment.getId())
                            .paymentUrl(payment.getPaymentUrl())
                            .status("FAILED")
                            .message("Stripe payment will fail (simulated)")
                            .build();
                }
                case RAZORPAY -> {
                    PaymentGateway gateway = gatewayFactory.getGateway(PaymentProvider.RAZORPAY);

                    PaymentLinkRequest linkRequest = PaymentLinkRequest.builder()
                            .amount(request.getAmount())
                            .currency("INR")
                            .orderId(payment.getId())
                            .callbackUrl(baseUrl + "/api/payments/payment-success")
                            .customerName(request.getUserId())
                            .description("Ticket booking - " + request.getTicketId())
                            .build();

                    // External API call - not in transaction
                    PaymentLinkResponse linkResponse = gateway.createPaymentLink(linkRequest);

                    // Update payment in separate transaction
                    updatePaymentUrl(payment, linkResponse.getPaymentUrl(),
                            linkResponse.getGatewayOrderId(), linkResponse.getExpiresAt());

                    publishPaymentInitiated(payment);

                    return PaymentInitiateResponse.builder()
                            .paymentId(payment.getId())
                            .paymentUrl(linkResponse.getPaymentUrl())
                            .status("PENDING")
                            .message("Payment link created successfully")
                            .build();
                }
                case SIMULATED -> {
                    payment.setStatus(PaymentStatus.COMPLETED);
                    payment.setTransactionId(SIMULATED_TRANSACTION_PREFIX + UUID.randomUUID().toString());
                    paymentRepository.save(payment);
                    publishPaymentCompleted(payment);

                    return PaymentInitiateResponse.builder()
                            .paymentId(payment.getId())
                            .paymentUrl(null)
                            .status("COMPLETED")
                            .message("Payment simulated successfully")
                            .build();
                }
                default -> throw new IllegalArgumentException("Unsupported payment provider: " + provider);
            }
        } catch (Exception e) {
            log.error("Error initiating payment", e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Failed to initiate payment: " + e.getMessage());
            paymentRepository.save(payment);

            throw new PaymentGatewayException("Failed to initiate payment", e);
        }
    }

    private void handleStripeSimulatedFailure(Payment payment) {
        log.info("Stripe payment - simulating immediate failure");

        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason("Stripe payment declined (simulated failure for testing)");
        paymentRepository.save(payment);

        publishPaymentFailed(payment);
    }

    private void handleRazorpayPayment(Payment payment, TicketReservedEvent event) {
        try {
            PaymentGateway gateway = gatewayFactory.getGateway(PaymentProvider.RAZORPAY);

            // Build payment link request
            PaymentLinkRequest request = PaymentLinkRequest.builder()
                    .amount(event.getTotalAmount())
                    .currency("INR")
                    .orderId(payment.getId())
                    .callbackUrl(baseUrl + "/payment-success")
                    .customerName(event.getUserId()) // Would fetch actual name from user service
                    .description(String.format("Ticket booking for %s", event.getMovieName()))
                    .build();

            // Create payment link
            PaymentLinkResponse response = gateway.createPaymentLink(request);

            // Update payment with gateway details (will be auto-saved by @Transactional)
            payment.setPaymentUrl(response.getPaymentUrl());
            payment.setPaymentGatewayOrderId(response.getGatewayOrderId());
            payment.setPaymentExpiresAt(response.getExpiresAt());

            // Publish payment initiated event
            publishPaymentInitiated(payment);

        } catch (Exception e) {
            log.error("Error creating Razorpay payment link", e);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Failed to create payment link: " + e.getMessage());
            publishPaymentFailed(payment);
            throw new PaymentGatewayException("Failed to create Razorpay payment link", e);
        }
    }

    private void handleSimulatedPayment(Payment payment) {
        // Fallback simulation logic (for testing without real gateways)
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId(SIMULATED_TRANSACTION_PREFIX + UUID.randomUUID().toString());
        publishPaymentCompleted(payment);
    }

    /**
     * Handle webhook callback from payment gateway.
     */
    @Transactional
    public void handleWebhookCallback(String provider, String payload, String signature) {
        if (provider == null || payload == null) {
            throw new IllegalArgumentException("Provider and payload cannot be null");
        }

        log.info("Received webhook from provider: {}", provider);

        try {
            // Parse provider and get gateway
            PaymentProvider paymentProvider = PaymentProvider.fromString(provider);
            PaymentGateway gateway = gatewayFactory.getGateway(paymentProvider);

            if (!gateway.verifyWebhookSignature(payload, signature)) {
                log.error("Invalid webhook signature from provider: {}", provider);
                throw new InvalidWebhookSignatureException("Invalid webhook signature from provider: " + provider);
            }

            // Extract order ID from payload
            String gatewayOrderId = extractOrderId(payload, provider);

            if (gatewayOrderId == null) {
                log.error("Could not extract order ID from webhook payload");
                return;
            }

            // Find payment by gateway order ID
            Payment payment = paymentRepository.findByPaymentGatewayOrderId(gatewayOrderId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment", "gatewayOrderId", gatewayOrderId));

            // Check if this specific webhook was already processed (idempotency)
            if (signature != null && signature.equals(payment.getIdempotencyKey())) {
                log.info("Webhook already processed (duplicate): paymentId={}, signature={}", payment.getId(), signature);
                return;
            }

            // Additional check: if payment is not pending, it was already processed
            if (payment.getStatus() != PaymentStatus.PENDING) {
                log.info("Payment already processed: {}, status: {}", payment.getId(), payment.getStatus());
                return;
            }

            // Store idempotency key
            payment.setIdempotencyKey(signature);

            // Verify payment with gateway API (double-check)
            PaymentVerificationResult result = gateway.verifyPayment(gatewayOrderId);

            // Update payment based on verification result (will be auto-saved by @Transactional)
            if (result.isSuccess()) {
                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setTransactionId(result.getTransactionId());
                publishPaymentCompleted(payment);
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setFailureReason(result.getFailureReason());
                publishPaymentFailed(payment);
            }

        } catch (InvalidWebhookSignatureException | ResourceNotFoundException e) {
            // Rethrow known exceptions
            throw e;
        } catch (Exception e) {
            log.error("Error processing webhook", e);
            throw new PaymentGatewayException("Webhook processing failed", e);
        }
    }

    private String extractOrderId(String payload, String provider) {
        try {
            JsonNode json = objectMapper.readTree(payload);

            if ("RAZORPAY".equalsIgnoreCase(provider)) {
                // Razorpay webhook structure: payload.payment.entity.order_id
                JsonNode paymentEntity = json.path("payload").path("payment").path("entity");
                if (paymentEntity.has("order_id")) {
                    return paymentEntity.get("order_id").asText();
                }
            }

            return null;
        } catch (Exception e) {
            log.error("Error extracting order ID from webhook payload", e);
            return null;
        }
    }

    private void publishPaymentInitiated(Payment payment) {
        PaymentInitiatedEvent event = PaymentInitiatedEvent.builder()
                .eventType(EVENT_TYPE_PAYMENT_INITIATED)
                .timestamp(java.time.LocalDateTime.now())
                .paymentId(payment.getId())
                .ticketId(payment.getTicketId())
                .userId(payment.getUserId())
                .paymentUrl(payment.getPaymentUrl())
                .paymentProvider(payment.getPaymentProvider().name())
                .amount(payment.getAmount())
                .expiresAt(payment.getPaymentExpiresAt())
                .correlationId(payment.getCorrelationId())
                .build();

        eventProducer.publishPaymentInitiated(event);
    }

    private void publishPaymentCompleted(Payment payment) {
        log.info("Payment successful: paymentId={}", payment.getId());

        PaymentCompletedEvent event = new PaymentCompletedEvent(
                payment.getId(),
                payment.getTicketId(),
                payment.getAmount(),
                payment.getPaymentMethod(),
                payment.getUserId(),
                payment.getTransactionId(),
                payment.getCorrelationId()
        );

        eventProducer.publishPaymentCompleted(event);
    }

    private void publishPaymentFailed(Payment payment) {
        log.warn("Payment failed: paymentId={}, reason={}", payment.getId(), payment.getFailureReason());

        PaymentFailedEvent event = new PaymentFailedEvent(
                payment.getId(),
                payment.getTicketId(),
                payment.getAmount(),
                payment.getUserId(),
                payment.getFailureReason(),
                payment.getCorrelationId()
        );

        eventProducer.publishPaymentFailed(event);
    }

    /**
     * Create payment record in separate transaction to avoid holding connection during external API calls.
     */
    @Transactional
    protected Payment createPaymentRecordSync(PaymentInitiateRequest request, PaymentProvider provider) {
        Payment payment = Payment.builder()
                .ticketId(request.getTicketId())
                .userId(request.getUserId())
                .amount(request.getAmount())
                .status(PaymentStatus.PENDING)
                .paymentMethod(PAYMENT_METHOD_ONLINE)
                .paymentProvider(provider)
                .correlationId(UUID.randomUUID().toString())
                .build();

        return paymentRepository.save(payment);
    }

    /**
     * Update payment URL in separate transaction after external API call.
     */
    @Transactional
    protected void updatePaymentUrl(Payment payment, String paymentUrl, String gatewayOrderId, java.time.LocalDateTime expiresAt) {
        payment.setPaymentUrl(paymentUrl);
        payment.setPaymentGatewayOrderId(gatewayOrderId);
        payment.setPaymentExpiresAt(expiresAt);
        paymentRepository.save(payment);
    }

    /**
     * Update payment as failed in separate transaction.
     */
    @Transactional
    protected void updatePaymentFailed(Payment payment, String failureReason, String paymentUrl) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(failureReason);
        payment.setPaymentUrl(paymentUrl);
        paymentRepository.save(payment);
    }

    /**
     * Update payment as completed in separate transaction.
     */
    @Transactional
    protected void updatePaymentCompleted(Payment payment, String transactionId) {
        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setTransactionId(transactionId);
        paymentRepository.save(payment);
    }
}
