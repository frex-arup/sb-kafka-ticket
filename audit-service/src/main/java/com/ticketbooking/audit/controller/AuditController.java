package com.ticketbooking.audit.controller;

import com.ticketbooking.audit.model.AuditLog;
import com.ticketbooking.audit.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Audit API - Query the event audit trail.
 *
 * Provides endpoints to:
 * - Get all events by correlation ID (trace a complete flow)
 * - Get events by type
 * - Get events by topic
 * - Get all events (paginated)
 */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuditController {

    private final AuditService auditService;

    @GetMapping("/correlation/{correlationId}")
    public ResponseEntity<List<AuditLog>> getEventsByCorrelationId(@PathVariable String correlationId) {
        return ResponseEntity.ok(auditService.getEventsByCorrelationId(correlationId));
    }

    @GetMapping("/type/{eventType}")
    public ResponseEntity<List<AuditLog>> getEventsByType(@PathVariable String eventType) {
        return ResponseEntity.ok(auditService.getEventsByType(eventType));
    }

    @GetMapping("/topic/{topic}")
    public ResponseEntity<List<AuditLog>> getEventsByTopic(@PathVariable String topic) {
        return ResponseEntity.ok(auditService.getEventsByTopic(topic));
    }

    @GetMapping
    public ResponseEntity<Page<AuditLog>> getAllEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return ResponseEntity.ok(auditService.getAllEvents(PageRequest.of(page, size)));
    }
}
