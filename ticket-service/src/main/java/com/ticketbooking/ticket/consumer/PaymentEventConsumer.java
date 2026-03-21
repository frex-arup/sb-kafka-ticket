package com.ticketbooking.ticket.consumer;

import com.ticketbooking.common.event.PaymentCompletedEvent;
import com.ticketbooking.common.event.PaymentFailedEvent;
import com.ticketbooking.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer for payment events.
 *
 * This demonstrates the consumer side of the SAGA pattern.
 * The Ticket Service listens for payment results and reacts accordingly:
 * - Payment success → Confirm booking
 * - Payment failure → Release tickets (compensating transaction)
 *
 * Learning Note: @KafkaListener at class level with @KafkaHandler methods
 * enables polymorphic event handling. Spring routes each message to the
 * appropriate @KafkaHandler based on the payload type.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@KafkaListener(
        topics = "${kafka.topics.payment-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
)
public class PaymentEventConsumer {

    private final TicketService ticketService;

    /**
     * Handle payment completed events.
     * This continues the SAGA to confirm the booking.
     */
    @KafkaHandler
    public void handlePaymentCompleted(@Payload PaymentCompletedEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Received PaymentCompletedEvent: paymentId={}, ticketId={}, correlationId={}",
                    event.getPaymentId(), event.getTicketId(), event.getCorrelationId());

            // Confirm the booking
            ticketService.confirmBooking(event);

            // Acknowledge message - mark as successfully processed
            acknowledgment.acknowledge();
            log.info("PaymentCompletedEvent processed successfully");

        } catch (Exception e) {
            log.error("Error processing PaymentCompletedEvent", e);
            // Don't acknowledge - message will be retried
            // In production, after max retries, send to DLQ
        }
    }

    /**
     * Handle payment failed events.
     * This triggers the compensating transaction to release the tickets.
     */
    @KafkaHandler
    public void handlePaymentFailed(@Payload PaymentFailedEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Received PaymentFailedEvent: paymentId={}, ticketId={}, reason={}, correlationId={}",
                    event.getPaymentId(), event.getTicketId(),
                    event.getFailureReason(), event.getCorrelationId());

            // Release the tickets (compensating transaction)
            ticketService.releaseTicket(event);

            // Acknowledge message
            acknowledgment.acknowledge();
            log.info("PaymentFailedEvent processed successfully");

        } catch (Exception e) {
            log.error("Error processing PaymentFailedEvent", e);
            // Don't acknowledge - message will be retried
        }
    }
}
