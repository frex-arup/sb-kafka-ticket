package com.ticketbooking.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Event published when payment processing fails.
 *
 * This triggers a compensating transaction in the SAGA pattern.
 * After this event:
 * 1. Ticket Service will consume it and RELEASE the reserved tickets
 * 2. Notification Service will send failure notification to user
 * 3. User Service will update booking status to failed
 * 4. Audit Service will log it
 *
 * Learning Note: This demonstrates the "compensating transaction" pattern.
 * When a step in a SAGA fails, we need to undo previous successful steps.
 * In this case, we release the tickets that were reserved.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentFailedEvent extends BaseEvent {

    private String paymentId;
    private String ticketId;
    private BigDecimal amount;
    private String userId;
    private String failureReason;

    public PaymentFailedEvent(String paymentId, String ticketId, BigDecimal amount,
                              String userId, String failureReason, String correlationId) {
        this.paymentId = paymentId;
        this.ticketId = ticketId;
        this.amount = amount;
        this.userId = userId;
        this.failureReason = failureReason;
        this.setCorrelationId(correlationId);
        initializeBaseFields("payment.failed");
    }
}
