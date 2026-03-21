package com.ticketbooking.payment.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Result of payment verification from payment gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentVerificationResult {
    private boolean success;
    private String transactionId;
    private BigDecimal amount;
    private String status;
    private String failureReason;
    private Map<String, Object> metadata;
}
