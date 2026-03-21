package com.ticketbooking.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentInitiateRequest {
    private String ticketId;
    private String userId;
    private BigDecimal amount;
    private String paymentProvider; // RAZORPAY, STRIPE, SIMULATED
}
