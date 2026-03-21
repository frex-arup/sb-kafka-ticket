package com.ticketbooking.payment.consumer;

import com.ticketbooking.common.event.TicketReservedEvent;
import com.ticketbooking.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer for ticket events.
 *
 * Listens for ticket.reserved events and triggers payment processing.
 *
 * Learning Note: This shows pure event-driven processing. The Payment
 * Service doesn't have a REST API for payments - it only reacts to events.
 * This loose coupling allows independent scaling and deployment.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketEventConsumer {

    private final PaymentService paymentService;

    @KafkaListener(
            topics = "${kafka.topics.ticket-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleTicketReserved(@Payload TicketReservedEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Received TicketReservedEvent: ticketId={}, amount={}, correlationId={}",
                    event.getTicketId(), event.getTotalAmount(), event.getCorrelationId());

            // Process payment
            paymentService.processPayment(event);

            // Acknowledge message
            acknowledgment.acknowledge();
            log.info("TicketReservedEvent processed successfully");

        } catch (Exception e) {
            log.error("Error processing TicketReservedEvent", e);
            // Don't acknowledge - will be retried
        }
    }
}
