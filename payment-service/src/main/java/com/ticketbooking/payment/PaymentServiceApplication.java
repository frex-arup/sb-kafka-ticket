package com.ticketbooking.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Payment Service - Processes payments for ticket reservations.
 *
 * This service demonstrates the consumer-producer pattern in a SAGA:
 * 1. Consumes "ticket.reserved" events
 * 2. Processes payment (simulated with 80% success rate)
 * 3. Produces "payment.completed" or "payment.failed" events
 *
 * Key Learning: This service is purely event-driven. It has no REST API
 * for payment processing - it only responds to events. This shows how
 * services can coordinate without direct coupling.
 */
@SpringBootApplication(scanBasePackages = {"com.ticketbooking.payment", "com.ticketbooking.common"})
@EnableKafka
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
