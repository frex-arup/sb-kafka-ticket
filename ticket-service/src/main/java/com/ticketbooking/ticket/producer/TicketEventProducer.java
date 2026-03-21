package com.ticketbooking.ticket.producer;

import com.ticketbooking.common.event.TicketBookedEvent;
import com.ticketbooking.common.event.TicketReleasedEvent;
import com.ticketbooking.common.event.TicketReservedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Kafka Producer for publishing ticket events.
 *
 * This demonstrates how to publish events to Kafka topics.
 * The CompletableFuture pattern allows async handling of send results.
 *
 * Learning Note: Each event is published to a topic where multiple
 * consumers can independently react to it. This is the pub/sub pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TicketEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.ticket-events}")
    private String ticketEventsTopic;

    /**
     * Publish ticket reserved event.
     * This kicks off the booking SAGA.
     */
    public void publishTicketReserved(TicketReservedEvent event) {
        log.info("Publishing TicketReservedEvent: ticketId={}, correlationId={}",
                event.getTicketId(), event.getCorrelationId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(ticketEventsTopic, event.getTicketId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("TicketReservedEvent published successfully: partition={}, offset={}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish TicketReservedEvent", ex);
            }
        });
    }

    /**
     * Publish ticket booked event.
     * This completes a successful booking SAGA.
     */
    public void publishTicketBooked(TicketBookedEvent event) {
        log.info("Publishing TicketBookedEvent: bookingId={}, correlationId={}",
                event.getBookingId(), event.getCorrelationId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(ticketEventsTopic, event.getTicketId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("TicketBookedEvent published successfully");
            } else {
                log.error("Failed to publish TicketBookedEvent", ex);
            }
        });
    }

    /**
     * Publish ticket released event.
     * This is a compensating transaction when booking fails.
     */
    public void publishTicketReleased(TicketReleasedEvent event) {
        log.info("Publishing TicketReleasedEvent: ticketId={}, reason={}, correlationId={}",
                event.getTicketId(), event.getReason(), event.getCorrelationId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(ticketEventsTopic, event.getTicketId(), event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("TicketReleasedEvent published successfully");
            } else {
                log.error("Failed to publish TicketReleasedEvent", ex);
            }
        });
    }
}
