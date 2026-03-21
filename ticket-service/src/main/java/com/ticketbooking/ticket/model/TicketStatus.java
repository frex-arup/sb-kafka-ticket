package com.ticketbooking.ticket.model;

/**
 * Ticket status enum representing the state in the booking SAGA.
 *
 * AVAILABLE: Tickets are available for booking
 * RESERVED: Tickets are reserved, waiting for payment
 * BOOKED: Payment completed, booking confirmed
 * RELEASED: Reservation cancelled/failed, tickets released
 */
public enum TicketStatus {
    AVAILABLE,
    RESERVED,
    BOOKED,
    RELEASED
}
