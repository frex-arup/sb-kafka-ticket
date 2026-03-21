package com.ticketbooking.notification.consumer;

import com.ticketbooking.common.event.*;
import com.ticketbooking.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer for ticket events.
 *
 * This demonstrates the "fan-out" pattern where multiple event types
 * are consumed by the same service using @KafkaHandler for polymorphic dispatch.
 *
 * Learning Note: Notice how the Notification Service doesn't produce
 * any events - it's a pure consumer. This is common for cross-cutting
 * concerns like notifications, logging, metrics, etc.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@KafkaListener(
        topics = "${kafka.topics.ticket-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
)
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaHandler
    public void handleTicketReserved(@Payload TicketReservedEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Received TicketReservedEvent: ticketId={}", event.getTicketId());
            notificationService.sendReservationConfirmation(
                    event.getUserId(),
                    event.getTicketId(),
                    event.getMovieName()
            );
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing TicketReservedEvent", e);
        }
    }

    @KafkaHandler
    public void handleTicketBooked(@Payload TicketBookedEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Received TicketBookedEvent: bookingId={}", event.getBookingId());
            notificationService.sendBookingConfirmation(
                    event.getUserId(),
                    event.getConfirmationCode(),
                    "Movie" // Would need to include movie name in event
            );
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing TicketBookedEvent", e);
        }
    }

    @KafkaHandler
    public void handleTicketReleased(@Payload TicketReleasedEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Received TicketReleasedEvent: ticketId={}", event.getTicketId());
            // No notification needed for release (already notified about payment failure)
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing TicketReleasedEvent", e);
        }
    }
}
