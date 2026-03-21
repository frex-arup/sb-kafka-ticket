package com.ticketbooking.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when reserved tickets are released (compensating transaction).
 *
 * This event is published when:
 * 1. Payment fails - tickets need to be released back to inventory
 * 2. Reservation timeout - user didn't complete payment in time
 *
 * After this event:
 * 1. Notification Service may notify user
 * 2. User Service updates booking status
 * 3. Audit Service logs the release
 *
 * Learning Note: This is part of the compensating transaction pattern.
 * When a SAGA fails, we need to undo what was done. The tickets go back
 * to being available for other users to book.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketReleasedEvent extends BaseEvent {

    private String ticketId;
    private String userId;
    private String reason; // PAYMENT_FAILED, TIMEOUT, etc.

    public TicketReleasedEvent(String ticketId, String userId, String reason,
                               String correlationId) {
        this.ticketId = ticketId;
        this.userId = userId;
        this.reason = reason;
        this.setCorrelationId(correlationId);
        initializeBaseFields("ticket.released");
    }
}
