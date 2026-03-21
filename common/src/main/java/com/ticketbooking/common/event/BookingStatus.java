package com.ticketbooking.common.event;

/**
 * Standard booking status enum to replace stringly-typed status fields.
 * Provides compile-time safety and IDE autocomplete support.
 */
public enum BookingStatus {
    RESERVED,
    BOOKED,
    RELEASED,
    CANCELLED
}
