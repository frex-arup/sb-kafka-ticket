package com.ticketbooking.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Event published when a new user registers.
 *
 * After this event:
 * 1. Notification Service sends welcome email
 * 2. Audit Service logs registration
 *
 * Learning Note: This is a simple event that demonstrates the fan-out pattern.
 * Multiple services can react to user registration independently.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserRegisteredEvent extends BaseEvent {

    private String userId;
    private String name;
    private String email;

    public UserRegisteredEvent(String userId, String name, String email, String correlationId) {
        this.userId = userId;
        this.name = name;
        this.email = email;
        this.setCorrelationId(correlationId);
        initializeBaseFields("user.registered");
    }
}
