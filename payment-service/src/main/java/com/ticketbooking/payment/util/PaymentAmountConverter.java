package com.ticketbooking.payment.util;

import java.math.BigDecimal;

/**
 * Utility class for payment amount conversions.
 *
 * Centralizes currency conversion logic to prevent errors and duplication.
 * Razorpay uses paise (smallest currency unit): 1 INR = 100 paise
 */
public final class PaymentAmountConverter {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private PaymentAmountConverter() {
        // Prevent instantiation
    }

    /**
     * Converts INR amount to paise (smallest unit for Razorpay).
     *
     * @param amount Amount in INR (e.g., 150.00)
     * @return Amount in paise (e.g., 15000)
     */
    public static int convertToPaise(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount cannot be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        return amount.multiply(HUNDRED).intValue();
    }

    /**
     * Converts paise amount to INR.
     *
     * @param paiseAmount Amount in paise (e.g., 15000)
     * @return Amount in INR (e.g., 150.00)
     */
    public static BigDecimal convertFromPaise(long paiseAmount) {
        if (paiseAmount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        return new BigDecimal(paiseAmount).divide(HUNDRED);
    }
}
