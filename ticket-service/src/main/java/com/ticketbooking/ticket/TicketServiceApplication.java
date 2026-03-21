package com.ticketbooking.ticket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Ticket Service - Manages ticket inventory and reservations.
 *
 * This service demonstrates the Orchestration SAGA pattern where it:
 * 1. Receives REST API requests to reserve tickets
 * 2. Produces "ticket.reserved" event to Kafka
 * 3. Listens for "payment.completed" or "payment.failed" events
 * 4. Updates ticket status and produces "ticket.booked" or "ticket.released" events
 *
 * Key Learning: This service is both a producer AND consumer, showing
 * how services coordinate through events rather than direct API calls.
 */
@SpringBootApplication(scanBasePackages = {"com.ticketbooking.ticket", "com.ticketbooking.common"})
@EnableKafka
public class TicketServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TicketServiceApplication.class, args);
    }
}
