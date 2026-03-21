package com.ticketbooking.notification.consumer;

import com.ticketbooking.common.event.UserRegisteredEvent;
import com.ticketbooking.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer for user events.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = "${kafka.topics.user-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleUserRegistered(@Payload UserRegisteredEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Received UserRegisteredEvent: userId={}", event.getUserId());
            notificationService.sendWelcomeEmail(
                    event.getUserId(),
                    event.getName(),
                    event.getEmail()
            );
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing UserRegisteredEvent", e);
        }
    }
}
