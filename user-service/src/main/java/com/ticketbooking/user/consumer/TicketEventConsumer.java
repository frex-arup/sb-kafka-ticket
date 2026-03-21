package com.ticketbooking.user.consumer;

import com.ticketbooking.common.event.*;
import com.ticketbooking.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@KafkaListener(
        topics = "${kafka.topics.ticket-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
)
public class TicketEventConsumer {
    private final UserService userService;

    @KafkaHandler
    public void handleTicketBooked(@Payload TicketBookedEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Received TicketBookedEvent: {}", event.getBookingId());
            userService.handleTicketBooked(event);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing TicketBookedEvent", e);
        }
    }

    @KafkaHandler
    public void handleTicketReleased(@Payload TicketReleasedEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Received TicketReleasedEvent: {}", event.getTicketId());
            userService.handleTicketReleased(event);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing TicketReleasedEvent", e);
        }
    }
}
