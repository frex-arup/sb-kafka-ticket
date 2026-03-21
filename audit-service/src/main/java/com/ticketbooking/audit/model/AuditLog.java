package com.ticketbooking.audit.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Audit Log MongoDB document.
 *
 * Stores complete event information for audit trail and event sourcing.
 *
 * Learning Note: Using MongoDB (document store) is ideal for event sourcing
 * because events can have varying structures. PostgreSQL would require
 * complex JSON columns or multiple tables.
 */
@Document(collection = "audit_logs")
@CompoundIndex(name = "correlation_idx", def = "{'correlationId': 1}")
@CompoundIndex(name = "eventType_idx", def = "{'eventType': 1}")
@CompoundIndex(name = "topic_idx", def = "{'topic': 1}")
@CompoundIndex(name = "timestamp_idx", def = "{'timestamp': -1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    private String id;

    private String eventId;
    private String eventType;
    private String correlationId;
    private String topic;
    private Integer partition;
    private Long offset;

    // Store entire event as flexible document
    private Map<String, Object> eventData;

    private LocalDateTime timestamp;
    private LocalDateTime auditedAt;
}
