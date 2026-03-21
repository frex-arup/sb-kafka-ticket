package com.ticketbooking.payment.gateway.impl;

import com.razorpay.Order;
import com.razorpay.PaymentLink;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import com.ticketbooking.payment.exception.PaymentGatewayException;
import com.ticketbooking.payment.gateway.PaymentGateway;
import com.ticketbooking.payment.gateway.dto.PaymentLinkRequest;
import com.ticketbooking.payment.gateway.dto.PaymentLinkResponse;
import com.ticketbooking.payment.gateway.dto.PaymentVerificationResult;
import com.ticketbooking.payment.util.PaymentAmountConverter;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

/**
 * Razorpay payment gateway implementation.
 *
 * Integrates with Razorpay Payment Links API for payment processing.
 * Handles payment link creation, webhook signature verification, and payment verification.
 */
@Slf4j
@Component
public class RazorpayPaymentGateway implements PaymentGateway {

    private static final String RAZORPAY_STATUS_PAID = "paid";

    private final RazorpayClient razorpayClient;
    private final String webhookSecret;
    private final int paymentLinkExpiryMinutes;

    public RazorpayPaymentGateway(
            @Value("${payment.razorpay.key-id}") String keyId,
            @Value("${payment.razorpay.key-secret}") String keySecret,
            @Value("${payment.razorpay.webhook-secret}") String webhookSecret,
            @Value("${payment.razorpay.payment-link-expiry-minutes:15}") int paymentLinkExpiryMinutes
    ) throws RazorpayException {
        this.razorpayClient = new RazorpayClient(keyId, keySecret);
        this.webhookSecret = webhookSecret;
        this.paymentLinkExpiryMinutes = paymentLinkExpiryMinutes;
        log.info("Razorpay Payment Gateway initialized (link expiry: {} minutes)", paymentLinkExpiryMinutes);
    }

    @Override
    public PaymentLinkResponse createPaymentLink(PaymentLinkRequest request) {
        try {
            log.info("Creating Razorpay payment link for order: {}", request.getOrderId());

            // Step 1: Create Razorpay Order
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", PaymentAmountConverter.convertToPaise(request.getAmount()));
            orderRequest.put("currency", request.getCurrency());
            orderRequest.put("receipt", request.getOrderId());

            Order order = razorpayClient.orders.create(orderRequest);
            String orderId = order.get("id");

            log.info("Razorpay order created: {}", orderId);

            // Step 2: Create Payment Link
            JSONObject paymentLinkRequest = new JSONObject();
            paymentLinkRequest.put("amount", PaymentAmountConverter.convertToPaise(request.getAmount()));
            paymentLinkRequest.put("currency", request.getCurrency());
            paymentLinkRequest.put("description", request.getDescription());
            paymentLinkRequest.put("reference_id", orderId);

            // Customer details
            JSONObject customer = new JSONObject();
            if (request.getCustomerName() != null) {
                customer.put("name", request.getCustomerName());
            }
            if (request.getCustomerEmail() != null) {
                customer.put("email", request.getCustomerEmail());
            }
            if (request.getCustomerPhone() != null) {
                customer.put("contact", request.getCustomerPhone());
            }
            if (customer.length() > 0) {
                paymentLinkRequest.put("customer", customer);
            }

            // Callback URL
            JSONObject notify = new JSONObject();
            notify.put("webhook", true);
            paymentLinkRequest.put("notify", notify);

            paymentLinkRequest.put("callback_url", request.getCallbackUrl());
            paymentLinkRequest.put("callback_method", "get");

            PaymentLink paymentLink = razorpayClient.paymentLink.create(paymentLinkRequest);
            String shortUrl = paymentLink.get("short_url");

            log.info("Razorpay payment link created: {}", shortUrl);

            // Payment link expiry configured via properties
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(paymentLinkExpiryMinutes);

            return PaymentLinkResponse.builder()
                    .paymentUrl(shortUrl)
                    .gatewayOrderId(orderId)
                    .expiresAt(expiresAt)
                    .build();

        } catch (RazorpayException e) {
            log.error("Error creating Razorpay payment link", e);
            throw new PaymentGatewayException("Failed to create Razorpay payment link: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        try {
            log.debug("Verifying Razorpay webhook signature");

            // Verify using Razorpay SDK
            boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);

            if (isValid) {
                log.info("Razorpay webhook signature verified successfully");
            } else {
                log.warn("Invalid Razorpay webhook signature");
            }

            return isValid;

        } catch (Exception e) {
            log.error("Error verifying Razorpay webhook signature", e);
            return false;
        }
    }

    @Override
    public PaymentVerificationResult verifyPayment(String gatewayOrderId) {
        try {
            log.info("Verifying payment for Razorpay order: {}", gatewayOrderId);

            // Fetch order details from Razorpay
            Order order = razorpayClient.orders.fetch(gatewayOrderId);
            String status = order.get("status");
            int amountInPaise = order.get("amount");
            java.math.BigDecimal amount = PaymentAmountConverter.convertFromPaise(amountInPaise);

            log.info("Razorpay order status: {}", status);

            boolean success = RAZORPAY_STATUS_PAID.equalsIgnoreCase(status);
            String transactionId = gatewayOrderId; // Use order ID as transaction ID

            // If order is paid, fetch payment details
            if (success) {
                try {
                    JSONObject payments = order.get("payments");
                    if (payments != null && payments.has("items") && payments.getJSONArray("items").length() > 0) {
                        JSONObject paymentObj = payments.getJSONArray("items").getJSONObject(0);
                        transactionId = paymentObj.getString("id");
                    }
                } catch (Exception e) {
                    log.warn("Could not extract payment ID, using order ID instead", e);
                }
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("orderId", gatewayOrderId);
            metadata.put("status", status);

            return PaymentVerificationResult.builder()
                    .success(success)
                    .transactionId(transactionId)
                    .amount(amount)
                    .status(status)
                    .failureReason(success ? null : "Payment not completed")
                    .metadata(metadata)
                    .build();

        } catch (RazorpayException e) {
            log.error("Error verifying Razorpay payment", e);
            return PaymentVerificationResult.builder()
                    .success(false)
                    .failureReason("Failed to verify payment: " + e.getMessage())
                    .build();
        }
    }
}
