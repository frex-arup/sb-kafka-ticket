package com.ticketbooking.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published when a user reserves tickets.
 *
 * This is the first event in the booking SAGA pattern.
 * After this event is published:
 * 1. Payment Service will consume it and attempt payment
 * 2. Notification Service may notify user of reservation
 * 3. Audit Service will log it
 *
 * Learning Note: This demonstrates the "publish-subscribe" pattern where
 * one event can trigger multiple consumers independently.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class TicketReservedEvent extends BaseEvent {

    private String ticketId;
    private String movieName;
    private LocalDateTime showTime;
    private List<String> seatNumbers;
    private String userId;
    private BigDecimal totalAmount;
    private LocalDateTime reservedUntil;
    private String paymentProvider;  // "RAZORPAY", "STRIPE", or "SIMULATED"
}
