package com.ticketbooking.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Audit Service - Event sourcing and audit trail.
 *
 * This service demonstrates the "Event Sourcing" pattern:
 * - Subscribes to ALL topics using pattern matching
 * - Stores every event in MongoDB
 * - Creates complete audit trail for compliance
 * - Enables event replay and debugging
 *
 * Key Learning: By storing all events, you can rebuild system state,
 * debug issues, and maintain compliance records.
 */
@SpringBootApplication(scanBasePackages = {"com.ticketbooking.audit", "com.ticketbooking.common"})
@EnableKafka
public class AuditServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuditServiceApplication.class, args);
    }
}
