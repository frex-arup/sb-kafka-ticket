package com.ticketbooking.audit.consumer;

import com.ticketbooking.audit.service.AuditService;
import com.ticketbooking.common.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Audit Event Consumer - Subscribes to ALL topics.
 *
 * Learning Note: This demonstrates subscribing to multiple topics.
 * In production, you could use pattern-based subscription to match
 * all topics: @KafkaListener(topicPattern = ".*-events")
 *
 * For this demo, we explicitly list all topics to be clear about
 * what's being audited.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventConsumer {

    private final AuditService auditService;

    @KafkaListener(
            topics = {
                    "${kafka.topics.ticket-events}",
                    "${kafka.topics.payment-events}",
                    "${kafka.topics.user-events}"
            },
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void auditEvent(
            @Payload BaseEvent event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header(KafkaHeaders.OFFSET) Long offset,
            Acknowledgment acknowledgment) {

        try {
            log.info("Auditing event: type={}, from topic={}, partition={}, offset={}",
                    event.getEventType(), topic, partition, offset);

            auditService.logEvent(event, topic, partition, offset);
            acknowledgment.acknowledge();

        } catch (Exception e) {
            log.error("Error auditing event from topic: {}", topic, e);
        }
    }
}
