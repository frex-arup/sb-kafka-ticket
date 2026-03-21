# Kafka Patterns Reference

Quick reference for EDA patterns implemented in this application.

## SAGA Pattern

**Pattern**: Orchestrate distributed transactions across services using events.

**When to Use**: Multi-step business process across services that must succeed or fail as a unit.

**Implementation**:
```
Service A → Event → Service B → Event → Service C
                                      ↓
                            Success: Complete
                            Failure: Compensate
```

**Example in App**: Ticket Booking
```java
// Step 1: Reserve
ticketRepository.save(ticket);
eventProducer.publishTicketReserved(event);

// Step 2: Payment (different service)
if (paymentSuccessful) {
    eventProducer.publishPaymentCompleted(event);
} else {
    eventProducer.publishPaymentFailed(event);
}

// Step 3: Confirm or Compensate
@KafkaListener(topics = "payment-events")
public void handlePayment(PaymentEvent event) {
    if (event instanceof PaymentCompleted) {
        confirmBooking();  // Happy path
    } else {
        releaseTickets();  // Compensating transaction
    }
}
```

## Event Sourcing

**Pattern**: Store all state changes as events.

**When to Use**: Need complete audit trail, time travel queries, or event replay.

**Implementation**:
```java
@KafkaListener(topics = {"ticket-events", "payment-events", "user-events"})
public void auditEvent(BaseEvent event) {
    AuditLog log = AuditLog.builder()
        .eventId(event.getEventId())
        .eventType(event.getEventType())
        .eventData(serialize(event))
        .timestamp(event.getTimestamp())
        .build();

    auditRepository.save(log);
}
```

**Benefits**:
- Complete history
- Can rebuild state from events
- Audit compliance
- Debug capabilities

## Fan-Out / Pub-Sub

**Pattern**: One event triggers multiple independent consumers.

**When to Use**: Multiple services need to react to same event.

**Implementation**:
```java
// Producer (one place)
eventProducer.publishTicketBooked(event);

// Multiple Consumers (different services)
// Notification Service
@KafkaListener(topics = "ticket-events", groupId = "notification-service")
public void sendNotification(TicketBookedEvent event) { }

// User Service
@KafkaListener(topics = "ticket-events", groupId = "user-service")
public void updateHistory(TicketBookedEvent event) { }

// Audit Service
@KafkaListener(topics = "ticket-events", groupId = "audit-service")
public void logEvent(TicketBookedEvent event) { }
```

## Idempotency

**Pattern**: Safe to process same message multiple times.

**When to Use**: Always! Kafka can deliver duplicates.

**Implementation**:
```java
@KafkaListener(topics = "payment-events")
public void handlePayment(PaymentEvent event, Acknowledgment ack) {
    // Check if already processed
    if (processedEvents.contains(event.getEventId())) {
        log.warn("Duplicate event: {}", event.getEventId());
        ack.acknowledge();
        return;
    }

    // Process
    processPayment(event);

    // Track as processed
    processedEvents.add(event.getEventId());
    ack.acknowledge();
}
```

## Compensating Transaction

**Pattern**: Undo previous operations when later steps fail.

**When to Use**: Part of SAGA pattern for handling failures.

**Implementation**:
```java
// Original transaction
public void reserveTicket(TicketRequest request) {
    ticket.setStatus(RESERVED);
    ticketRepository.save(ticket);
    eventProducer.publishTicketReserved(event);
}

// Compensating transaction
@KafkaListener(topics = "payment-events")
public void handlePaymentFailed(PaymentFailedEvent event) {
    Ticket ticket = ticketRepository.findById(event.getTicketId());
    ticket.setStatus(RELEASED);  // Undo reservation
    ticketRepository.save(ticket);
    eventProducer.publishTicketReleased(event);
}
```

## Dead Letter Queue (DLQ)

**Pattern**: Handle messages that fail processing.

**When to Use**: Prevent poison messages from blocking consumption.

**Implementation** (to add):
```java
@KafkaListener(topics = "ticket-events")
public void handleTicket(TicketEvent event, Acknowledgment ack) {
    try {
        processEvent(event);
        ack.acknowledge();
    } catch (Exception e) {
        retryCount++;
        if (retryCount > MAX_RETRIES) {
            sendToDLQ(event, e);
            ack.acknowledge();
        }
        // Don't acknowledge - will retry
    }
}

private void sendToDLQ(Event event, Exception error) {
    kafkaTemplate.send("dead-letter-queue",
        new FailedMessage(event, error.getMessage()));
}
```

## Correlation ID Pattern

**Pattern**: Link related events across services.

**When to Use**: Trace complete flows through system.

**Implementation**:
```java
// Generate at entry point
String correlationId = UUID.randomUUID().toString();

// Include in all events
TicketReservedEvent event = new TicketReservedEvent(...);
event.setCorrelationId(correlationId);

// Propagate through entire flow
PaymentCompletedEvent paymentEvent = new PaymentCompletedEvent(...);
paymentEvent.setCorrelationId(ticketEvent.getCorrelationId());

// Query by correlation ID
List<AuditLog> flow = auditRepository.findByCorrelationId(correlationId);
// Returns: ticket.reserved → payment.completed → ticket.booked
```

## Partition Key Strategy

**Pattern**: Route related messages to same partition.

**When to Use**: Maintain ordering of related events.

**Implementation**:
```java
// Use entity ID as partition key
kafkaTemplate.send(topic, ticketId, event);

// All events for same ticket go to same partition
// Guarantees: ticket.reserved arrives before payment.completed for same ticket
```

## Consumer Group Pattern

**Pattern**: Scale consumers horizontally.

**When to Use**: Need to process high message volumes.

**Implementation**:
```yaml
spring:
  kafka:
    consumer:
      group-id: ticket-service-group
```

**How It Works**:
- 3 partitions, 1 consumer: consumes all 3
- 3 partitions, 3 consumers: each consumes 1
- 3 partitions, 5 consumers: 2 are idle

## Event Versioning

**Pattern**: Handle schema evolution.

**When to Use**: Events evolve over time.

**Implementation**:
```java
@Data
public class TicketReservedEvent extends BaseEvent {
    private String version = "2.0";  // Changed from 1.0

    // v1 fields
    private String ticketId;
    private BigDecimal amount;

    // v2 adds
    private String movieCategory;  // NEW in v2

    // Consumer handles both versions
    public String getCategory() {
        return movieCategory != null ? movieCategory : "STANDARD";
    }
}
```

## Transactional Outbox

**Pattern**: Guarantee DB write and event publish happen together.

**When to Use**: Need exactly-once semantics.

**Implementation** (not in this app, but important):
```java
@Transactional
public void reserveTicket(TicketRequest request) {
    // 1. Save to DB
    Ticket ticket = ticketRepository.save(new Ticket(...));

    // 2. Save event to outbox table (same transaction)
    OutboxEvent outbox = new OutboxEvent(
        "ticket.reserved",
        serialize(ticketReservedEvent)
    );
    outboxRepository.save(outbox);

    // 3. Background process reads outbox and publishes to Kafka
    // This ensures DB and Kafka are consistent
}
```

## Event Enrichment

**Pattern**: Add data to events as they flow through system.

**When to Use**: Downstream consumers need additional context.

**Implementation**:
```java
// Ticket Service publishes basic event
TicketReservedEvent event = new TicketReservedEvent(ticketId, userId, ...);

// Payment Service enriches with payment method
PaymentCompletedEvent enriched = new PaymentCompletedEvent(
    event.getTicketId(),
    paymentId,
    paymentMethod,  // Enriched
    transactionId   // Enriched
);
```

## Anti-Patterns to Avoid

### ❌ Synchronous Event Processing

```java
// BAD: Blocking wait for result
TicketReservedEvent event = produceTicketReserved();
PaymentResult result = waitForPaymentResult(event.getTicketId()); // BLOCKING!
```

**Why Bad**: Defeats the purpose of async messaging.

**Better**: React to events asynchronously via consumers.

### ❌ Events as RPC

```java
// BAD: Using events like remote procedure calls
public void processPayment(PaymentRequest request) {
    kafkaTemplate.send("payment-request-topic", request);
    // Wait for response...
}
```

**Why Bad**: Kafka isn't designed for request/response.

**Better**: Use events for one-way notifications, not RPC.

### ❌ Large Event Payloads

```java
// BAD: Including entire document
TicketReservedEvent event = new TicketReservedEvent(
    ticketId,
    fullUserProfile,      // 5KB of data
    movieDetails,         // 10KB of data
    entireSeatMap         // 50KB of data
);
```

**Why Bad**: Wastes bandwidth, slows processing.

**Better**: Include IDs, let consumers fetch details if needed.

### ❌ No Idempotency

```java
// BAD: Blindly processing without checking
@KafkaListener(topics = "payment-events")
public void charge(PaymentEvent event) {
    creditCard.charge(event.getAmount());  // Could charge twice!
}
```

**Why Bad**: Duplicate events will cause duplicate actions.

**Better**: Check event IDs, use idempotent operations.

## Pattern Selection Guide

| Scenario | Pattern |
|----------|---------|
| Multi-step transaction | SAGA |
| Need complete history | Event Sourcing |
| Multiple reactions to event | Fan-Out |
| Handle duplicates | Idempotency |
| Failure recovery | Compensating Transaction |
| Trace flow across services | Correlation ID |
| Scale consumers | Consumer Groups |
| Maintain ordering | Partition Keys |
| Schema changes | Event Versioning |
| Exactly-once | Transactional Outbox |

## Further Reading

- **Enterprise Integration Patterns**: Gregor Hohpe
- **Designing Event-Driven Systems**: Ben Stopford (Confluent)
- **Building Microservices**: Sam Newman
- **Domain-Driven Design**: Eric Evans

---

Use this as a reference when implementing new features or debugging issues!
