package com.ticketbooking.audit.repository;

import com.ticketbooking.audit.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends MongoRepository<AuditLog, String> {

    List<AuditLog> findByCorrelationId(String correlationId);

    List<AuditLog> findByEventType(String eventType);

    Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    List<AuditLog> findByTopic(String topic);
}
