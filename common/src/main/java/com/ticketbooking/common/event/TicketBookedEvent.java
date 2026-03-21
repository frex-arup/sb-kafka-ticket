package com.ticketbooking.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when a ticket booking is confirmed.
 *
 * This is the final event in a successful booking SAGA.
 * It's published after payment is completed.
 *
 * After this event:
 * 1. Notification Service sends booking confirmation to user
 * 2. User Service updates user's booking history
 * 3. Audit Service logs successful completion
 *
 * Learning Note: This event represents the successful completion of a
 * distributed transaction across multiple services. The booking is now
 * confirmed and cannot be cancelled automatically.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketBookedEvent extends BaseEvent {

    private String bookingId;
    private String ticketId;
    private String userId;
    private String confirmationCode;
    private String status; // CONFIRMED

    public TicketBookedEvent(String bookingId, String ticketId, String userId,
                             String confirmationCode, String status, String correlationId) {
        this.bookingId = bookingId;
        this.ticketId = ticketId;
        this.userId = userId;
        this.confirmationCode = confirmationCode;
        this.status = status;
        this.setCorrelationId(correlationId);
        initializeBaseFields("ticket.booked");
    }
}
