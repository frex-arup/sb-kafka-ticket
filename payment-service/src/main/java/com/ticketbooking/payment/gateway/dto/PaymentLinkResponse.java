package com.ticketbooking.payment.gateway.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response DTO containing payment link details from payment gateway.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentLinkResponse {
    private String paymentUrl;
    private String gatewayOrderId;
    private LocalDateTime expiresAt;
}
