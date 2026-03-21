package com.ticketbooking.payment.controller;

import com.ticketbooking.common.exception.ResourceNotFoundException;
import com.ticketbooking.payment.dto.PaymentInitiateRequest;
import com.ticketbooking.payment.dto.PaymentInitiateResponse;
import com.ticketbooking.payment.exception.InvalidWebhookSignatureException;
import com.ticketbooking.payment.model.Payment;
import com.ticketbooking.payment.model.PaymentProvider;
import com.ticketbooking.payment.repository.PaymentRepository;
import com.ticketbooking.payment.service.PaymentService;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Payment REST Controller for webhook handling and payment status queries.
 */
@RestController
@RequestMapping("/api/payments")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;

    /**
     * Initiate payment synchronously - for smooth UI flow.
     * Frontend calls this after ticket reservation to get payment URL immediately.
     */
    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiateResponse> initiatePayment(@RequestBody PaymentInitiateRequest request) {
        try {
            log.info("Payment initiation request received for ticket: {}", request.getTicketId());
            PaymentInitiateResponse response = paymentService.initiatePaymentSync(request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error initiating payment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(PaymentInitiateResponse.builder()
                            .status("ERROR")
                            .message("Failed to initiate payment: " + e.getMessage())
                            .build());
        }
    }

    /**
     * Razorpay webhook endpoint.
     * Razorpay sends payment status updates to this endpoint.
     */
    @PostMapping("/webhook/razorpay")
    public ResponseEntity<String> handleRazorpayWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature
    ) {
        try {
            log.info("Received Razorpay webhook");

            if (signature == null) {
                log.error("Missing X-Razorpay-Signature header");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing signature");
            }

            paymentService.handleWebhookCallback(PaymentProvider.RAZORPAY.name(), payload, signature);
            return ResponseEntity.ok("Webhook processed");

        } catch (InvalidWebhookSignatureException e) {
            log.error("Invalid webhook signature", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid signature");
        } catch (ResourceNotFoundException e) {
            log.error("Payment not found for webhook", e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Payment not found");
        } catch (Exception e) {
            log.error("Error processing Razorpay webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Webhook processing failed");
        }
    }

    /**
     * Get payment status by ticket ID.
     * Used by frontend to poll for payment URL and status.
     */
    @GetMapping("/ticket/{ticketId}/status")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatus(@PathVariable String ticketId) {
        try {
            log.debug("Fetching payment status for ticket: {}", ticketId);

            Payment payment = paymentRepository.findByTicketId(ticketId)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment", "ticketId", ticketId));

            PaymentStatusResponse response = PaymentStatusResponse.builder()
                    .paymentId(payment.getId())
                    .ticketId(payment.getTicketId())
                    .status(payment.getStatus().name())
                    .paymentProvider(payment.getPaymentProvider().name())
                    .paymentUrl(payment.getPaymentUrl())
                    .amount(payment.getAmount())
                    .transactionId(payment.getTransactionId())
                    .failureReason(payment.getFailureReason())
                    .createdAt(payment.getCreatedAt())
                    .updatedAt(payment.getUpdatedAt())
                    .build();

            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException e) {
            log.error("Payment not found", e);
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Error fetching payment status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Razorpay payment callback handler.
     * Razorpay redirects users here after payment completion.
     * This endpoint redirects back to the frontend.
     */
    @GetMapping("/payment-success")
    public ResponseEntity<Void> handlePaymentCallback(
            @RequestParam(required = false) String razorpay_payment_id,
            @RequestParam(required = false) String razorpay_payment_link_id,
            @RequestParam(required = false) String razorpay_payment_link_status
    ) {
        log.info("Payment callback received - paymentId: {}, status: {}", razorpay_payment_id, razorpay_payment_link_status);

        // Redirect to frontend with success message
        String redirectUrl = "http://localhost:4200/my-bookings?payment=success";

        return ResponseEntity.status(302)
                .header("Location", redirectUrl)
                .build();
    }

    /**
     * Stripe failure page (mock endpoint for simulated Stripe failure).
     * Shows error message when user is redirected here.
     */
    @GetMapping("/stripe-failure-page/{paymentId}")
    public ResponseEntity<String> stripeFailurePage(@PathVariable String paymentId) {
        log.info("Stripe failure page accessed for payment: {}", paymentId);

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Payment Failed</title>
                    <style>
                        body { font-family: Arial, sans-serif; text-align: center; padding: 50px; }
                        .error { color: #d32f2f; font-size: 24px; margin: 20px 0; }
                        .message { color: #666; margin: 20px 0; }
                    </style>
                </head>
                <body>
                    <h1 class="error">Payment Failed</h1>
                    <p class="message">Stripe payment declined (simulated failure for testing)</p>
                    <p>Payment ID: %s</p>
                    <p>Your ticket reservation has been released.</p>
                    <p><a href="http://localhost:4200">Return to booking</a></p>
                </body>
                </html>
                """.formatted(paymentId);

        return ResponseEntity.ok()
                .header("Content-Type", "text/html")
                .body(html);
    }

    /**
     * Payment status response DTO.
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class PaymentStatusResponse {
        private String paymentId;
        private String ticketId;
        private String status;
        private String paymentProvider;
        private String paymentUrl;
        private java.math.BigDecimal amount;
        private String transactionId;
        private String failureReason;
        private java.time.LocalDateTime createdAt;
        private java.time.LocalDateTime updatedAt;
    }
}
