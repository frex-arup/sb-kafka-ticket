package com.ticketbooking.payment.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Request DTO for creating payment links across different payment gateways.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLinkRequest {
    private BigDecimal amount;
    private String currency;
    private String orderId;
    private String callbackUrl;
    private String customerName;
    private String customerEmail;
    private String customerPhone;
    private String description;
}
