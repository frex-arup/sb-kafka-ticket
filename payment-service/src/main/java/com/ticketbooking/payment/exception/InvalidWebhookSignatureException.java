package com.ticketbooking.payment.exception;

/**
 * Exception thrown when webhook signature verification fails.
 * This indicates a potential security issue or misconfigured webhook secret.
 */
public class InvalidWebhookSignatureException extends RuntimeException {

    public InvalidWebhookSignatureException(String message) {
        super(message);
    }

    public InvalidWebhookSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
