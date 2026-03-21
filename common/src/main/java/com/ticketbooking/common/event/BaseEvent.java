package com.ticketbooking.common.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base event class that all domain events extend.
 *
 * This provides common metadata for all events in the system:
 * - eventId: Unique identifier for the event (used for idempotency)
 * - eventType: The type of event (e.g., "ticket.reserved", "payment.completed")
 * - timestamp: When the event was created
 * - correlationId: Links related events together across services
 * - version: Event schema version for backward compatibility
 *
 * Learning Note: In event-driven systems, having a consistent event structure
 * helps with debugging, tracing flows across services, and implementing patterns
 * like idempotency (ensuring duplicate events don't cause duplicate actions).
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {

    /**
     * Unique identifier for this event.
     * Used for idempotency - consumers can track processed event IDs
     * to avoid processing the same event twice.
     */
    private String eventId;

    /**
     * The type of event (e.g., "ticket.reserved", "payment.completed").
     * Helps consumers quickly identify what happened.
     */
    private String eventType;

    /**
     * When this event was created.
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;

    /**
     * Correlation ID links related events together.
     * For example, all events related to a single booking will share
     * the same correlationId, making it easy to trace the complete flow.
     */
    private String correlationId;

    /**
     * Event schema version for backward compatibility.
     * If event structure changes, version helps old consumers understand new events.
     */
    private String version;

    /**
     * Protected constructor to be called by subclasses.
     * Initializes all common event fields.
     */
    protected BaseEvent(String eventType, String correlationId) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
        this.correlationId = correlationId;
        this.version = "1.0";
    }

    /**
     * Initialize common fields with default values.
     * Call this in subclass constructors.
     * @deprecated Use constructor instead: super(eventType, correlationId)
     */
    @Deprecated
    protected void initializeBaseFields(String eventType) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.timestamp = LocalDateTime.now();
        this.version = "1.0";
        // correlationId should be set by the caller or inherited from request
    }
}
