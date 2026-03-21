package com.ticketbooking.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Event published when payment link is generated and ready for user.
 *
 * This event bridges the gap between payment initiation and completion,
 * allowing the frontend to redirect users to the payment gateway.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PaymentInitiatedEvent extends BaseEvent {

    private String paymentId;
    private String ticketId;
    private String userId;
    private String paymentUrl;
    private String paymentProvider;
    private BigDecimal amount;
    private LocalDateTime expiresAt;
}
