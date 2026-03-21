package com.ticketbooking.payment.gateway;

import com.ticketbooking.payment.gateway.dto.PaymentLinkRequest;
import com.ticketbooking.payment.gateway.dto.PaymentLinkResponse;
import com.ticketbooking.payment.gateway.dto.PaymentVerificationResult;

/**
 * Payment Gateway abstraction for supporting multiple payment providers.
 *
 * Implementations handle provider-specific payment link creation,
 * webhook signature verification, and payment verification.
 */
public interface PaymentGateway {

    /**
     * Creates a payment link with the payment gateway.
     *
     * @param request Payment link creation request with amount, customer details, etc.
     * @return Response containing payment URL and gateway order ID
     */
    PaymentLinkResponse createPaymentLink(PaymentLinkRequest request);

    /**
     * Verifies webhook signature to ensure authenticity.
     *
     * @param payload Raw webhook payload
     * @param signature Signature from webhook header
     * @return true if signature is valid, false otherwise
     */
    boolean verifyWebhookSignature(String payload, String signature);

    /**
     * Verifies payment status with the gateway API.
     *
     * @param gatewayOrderId Gateway-specific order/payment ID
     * @return Payment verification result with status and transaction details
     */
    PaymentVerificationResult verifyPayment(String gatewayOrderId);
}
