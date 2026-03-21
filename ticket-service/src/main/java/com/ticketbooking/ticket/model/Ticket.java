package com.ticketbooking.ticket.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ticket entity representing a ticket reservation/booking.
 *
 * Status flow:
 * AVAILABLE → RESERVED → BOOKED (success)
 *          → RESERVED → RELEASED (failure)
 *
 * Learning Note: The status field tracks where we are in the SAGA.
 * This is important for handling failures and ensuring consistency.
 */
@Entity
@Table(name = "tickets", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_status", columnList = "status"),
    @Index(name = "idx_user_status", columnList = "userId,status"),
    @Index(name = "idx_correlation", columnList = "correlationId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String movieName;

    @Column(nullable = false)
    private LocalDateTime showTime;

    @ElementCollection
    @CollectionTable(name = "ticket_seats", joinColumns = @JoinColumn(name = "ticket_id"))
    @Column(name = "seat_number")
    private List<String> seatNumbers;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TicketStatus status;

    private LocalDateTime reservedUntil;

    private String confirmationCode;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // Correlation ID for tracing across services
    private String correlationId;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
