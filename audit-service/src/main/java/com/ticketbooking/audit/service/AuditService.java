package com.ticketbooking.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ticketbooking.audit.model.AuditLog;
import com.ticketbooking.audit.repository.AuditLogRepository;
import com.ticketbooking.common.event.BaseEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Audit Service - Stores all events for compliance and debugging.
 *
 * This implements the Event Sourcing pattern where every state change
 * is recorded as an event. Benefits:
 * - Complete audit trail
 * - Can replay events to rebuild state
 * - Debug issues by examining event history
 * - Compliance and regulatory requirements
 */
@Service
@Slf4j
public class AuditService {

    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void logEvent(BaseEvent event, String topic, Integer partition, Long offset) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> eventData = objectMapper.convertValue(event, Map.class);

            AuditLog auditLog = AuditLog.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType())
                    .correlationId(event.getCorrelationId())
                    .topic(topic)
                    .partition(partition)
                    .offset(offset)
                    .eventData(eventData)
                    .timestamp(event.getTimestamp())
                    .auditedAt(LocalDateTime.now())
                    .build();

            repository.save(auditLog);
            log.info("Event audited: type={}, correlationId={}, topic={}",
                    event.getEventType(), event.getCorrelationId(), topic);

        } catch (Exception e) {
            log.error("Failed to audit event: {}", event.getEventType(), e);
        }
    }

    public List<AuditLog> getEventsByCorrelationId(String correlationId) {
        return repository.findByCorrelationId(correlationId);
    }

    public List<AuditLog> getEventsByType(String eventType) {
        return repository.findByEventType(eventType);
    }

    public Page<AuditLog> getAllEvents(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public List<AuditLog> getEventsByTopic(String topic) {
        return repository.findByTopic(topic);
    }
}
