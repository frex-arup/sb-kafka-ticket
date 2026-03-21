package com.ticketbooking.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Notification Service - Sends notifications for various events.
 *
 * This is a pure consumer service demonstrating the "fan-out" pattern:
 * - Multiple event types trigger notifications
 * - Stateless - no database needed
 * - Independent from other services
 *
 * Key Learning: This shows how one event (e.g., ticket.booked) can
 * trigger multiple consumers. The Notification Service is one of many
 * services reacting to the same event.
 */
@SpringBootApplication(scanBasePackages = {"com.ticketbooking.notification", "com.ticketbooking.common"})
@EnableKafka
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
