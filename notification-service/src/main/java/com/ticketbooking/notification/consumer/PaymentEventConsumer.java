package com.ticketbooking.notification.consumer;

import com.ticketbooking.common.event.PaymentCompletedEvent;
import com.ticketbooking.common.event.PaymentFailedEvent;
import com.ticketbooking.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Kafka Consumer for payment events.
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

    private final NotificationService notificationService;

    @KafkaHandler
    public void handlePaymentCompleted(@Payload PaymentCompletedEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Received PaymentCompletedEvent: paymentId={}", event.getPaymentId());
            notificationService.sendPaymentConfirmation(
                    event.getUserId(),
                    event.getTicketId(),
                    event.getTransactionId()
            );
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing PaymentCompletedEvent", e);
        }
    }

    @KafkaHandler
    public void handlePaymentFailed(@Payload PaymentFailedEvent event, Acknowledgment acknowledgment) {
        try {
            log.info("Received PaymentFailedEvent: paymentId={}", event.getPaymentId());
            notificationService.sendPaymentFailureNotification(
                    event.getUserId(),
                    event.getTicketId(),
                    event.getFailureReason()
            );
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error processing PaymentFailedEvent", e);
        }
    }
}
