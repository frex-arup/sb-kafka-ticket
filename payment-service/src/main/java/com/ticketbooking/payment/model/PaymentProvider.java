package com.ticketbooking.payment.model;

/**
 * Enum representing supported payment providers.
 *
 * Using enum instead of strings provides:
 * - Type safety at compile time
 * - Prevention of typos
 * - Clear documentation of supported providers
 * - Easy addition of provider-specific behavior
 */
public enum PaymentProvider {
    /**
     * Razorpay payment gateway - real integration with test mode
     */
    RAZORPAY,

    /**
     * Stripe payment gateway - simulated failure for testing compensating transactions
     */
    STRIPE,

    /**
     * Simulated payment for backward compatibility and testing
     */
    SIMULATED;

    /**
     * Parse payment provider from string value.
     *
     * @param value String value to parse
     * @return PaymentProvider enum
     * @throws IllegalArgumentException if value is invalid
     */
    public static PaymentProvider fromString(String value) {
        if (value == null) {
            return SIMULATED;
        }

        try {
            return PaymentProvider.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown payment provider: " + value +
                    ". Supported providers: RAZORPAY, STRIPE, SIMULATED");
        }
    }
}
