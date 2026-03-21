package com.ticketbooking.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Event published when payment is successfully processed.
 *
 * This is the second step in a successful booking SAGA.
 * After this event:
 * 1. Ticket Service will consume it and confirm the booking
 * 2. Notification Service will send payment confirmation
 * 3. User Service will update booking history
 * 4. Audit Service will log it
 *
 * Learning Note: This shows how events drive the workflow forward.
 * No service directly calls another - they communicate via events.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentCompletedEvent extends BaseEvent {

    private String paymentId;
    private String ticketId;
    private BigDecimal amount;
    private String paymentMethod;
    private String userId;
    private String transactionId;

    public PaymentCompletedEvent(String paymentId, String ticketId, BigDecimal amount,
                                 String paymentMethod, String userId, String transactionId,
                                 String correlationId) {
        this.paymentId = paymentId;
        this.ticketId = ticketId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.userId = userId;
        this.transactionId = transactionId;
        this.setCorrelationId(correlationId);
        initializeBaseFields("payment.completed");
    }
}
