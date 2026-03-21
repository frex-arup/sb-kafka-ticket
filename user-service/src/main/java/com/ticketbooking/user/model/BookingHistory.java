package com.ticketbooking.user.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "booking_history", indexes = {
    @Index(name = "idx_user_id", columnList = "userId"),
    @Index(name = "idx_ticket_id", columnList = "ticketId")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String ticketId;

    private String bookingId;

    @Column(nullable = false)
    private String status; // RESERVED, BOOKED, RELEASED

    private String confirmationCode;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
