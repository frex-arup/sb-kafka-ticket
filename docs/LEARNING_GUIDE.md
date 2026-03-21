# 📚 Kafka Learning Guide

A comprehensive guide to understanding Apache Kafka through this ticket booking application.

## Table of Contents

1. [Kafka Basics](#kafka-basics)
2. [Core Concepts](#core-concepts)
3. [Event-Driven Architecture](#event-driven-architecture)
4. [Patterns in This Application](#patterns-in-this-application)
5. [Hands-On Exercises](#hands-on-exercises)

## Kafka Basics

### What is Apache Kafka?

Apache Kafka is a distributed event streaming platform designed for:
- High-throughput message processing
- Real-time data pipelines
- Event sourcing and stream processing
- Decoupling microservices

**Key Characteristics:**
- **Distributed**: Runs on multiple servers
- **Scalable**: Handle millions of messages per second
- **Fault-tolerant**: Data replication prevents loss
- **Persistent**: Messages stored on disk

### Why Use Kafka?

**Traditional Approach** (Synchronous):
```
Service A → REST API → Service B → REST API → Service C
```
- Tight coupling
- If Service B is down, Service A fails
- Hard to scale independently

**Kafka Approach** (Asynchronous):
```
Service A → Kafka Topic → Service B
                       → Service C
                       → Service D
```
- Loose coupling
- Services don't know about each other
- Can add/remove consumers without changing producers
- Built-in retry and fault tolerance

## Core Concepts

### 1. Topics

**What**: A topic is a category or feed of messages.

**In Our App**:
- `ticket-events`: All ticket-related events
- `payment-events`: Payment processing events
- `user-events`: User registration events

**Analogy**: Like a YouTube channel. Producers publish videos (messages), subscribers watch them (consume).

**View Topics**:
```bash
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

### 2. Partitions

**What**: Topics are split into partitions for parallelism.

**Key Points**:
- Each partition is an ordered sequence of messages
- Messages in a partition have an offset (position)
- Partitions enable horizontal scaling
- Our `ticket-events` topic has 3 partitions

**Why Important**:
- Multiple consumers can read from different partitions simultaneously
- Partition key (e.g., ticketId) ensures related messages go to same partition
- Maintains ordering within a partition

**See Partition Distribution**:
1. Open Kafka UI (http://localhost:8080)
2. Click "Topics" → "ticket-events"
3. See message distribution across partitions

### 3. Producers

**What**: Applications that publish messages to Kafka.

**In Our App**:
- Ticket Service produces `ticket.reserved`, `ticket.booked`
- Payment Service produces `payment.completed`, `payment.failed`

**Code Example** (`TicketEventProducer.java`):
```java
kafkaTemplate.send(topicName, key, event);
```

**Key Configurations**:
- `acks=all`: Wait for all replicas (durability)
- `enable.idempotence=true`: Prevent duplicate messages
- `retries=3`: Retry failed sends

### 4. Consumers

**What**: Applications that read messages from Kafka.

**In Our App**:
- Payment Service consumes `ticket.reserved`
- Ticket Service consumes `payment.completed/failed`
- Notification Service consumes multiple event types

**Code Example** (`PaymentEventConsumer.java`):
```java
@KafkaListener(topics = "payment-events", groupId = "ticket-service-group")
public void handlePayment(PaymentCompletedEvent event) {
    // Process event
    acknowledgment.acknowledge(); // Manual commit
}
```

**Key Configurations**:
- `auto-offset-reset=earliest`: Start from beginning for new consumers
- `enable-auto-commit=false`: Manual control over when message is "consumed"
- Consumer group: Share load across multiple instances

### 5. Consumer Groups

**What**: Multiple consumers working together to consume a topic.

**How It Works**:
- Each partition is consumed by exactly ONE consumer in a group
- If you have 3 partitions and 3 consumers, each gets one partition
- If a consumer dies, its partitions are reassigned (rebalancing)

**In Our App**:
- `ticket-service-group`: Ticket Service consumers
- `payment-service-group`: Payment Service consumers
- `notification-service-group`: Notification consumers

**See Consumer Groups**:
1. Kafka UI → "Consumers"
2. See group members and partition assignments
3. Check lag (messages behind)

### 6. Offsets

**What**: The position of a consumer in a partition.

**How It Works**:
- Each message has an offset (0, 1, 2, 3...)
- Consumer tracks "where it is" in each partition
- On restart, continues from last committed offset

**Manual vs Auto Commit**:
- **Auto**: Kafka auto-commits offsets (can lead to message loss or duplicates)
- **Manual**: We control when to commit (used in this app)

## Event-Driven Architecture

### What is EDA?

Services communicate by producing and consuming events rather than making direct API calls.

**Benefits**:
1. **Loose Coupling**: Services don't know about each other
2. **Scalability**: Easy to add new consumers
3. **Resilience**: System works even if services are down
4. **Audit Trail**: All events are recorded
5. **Time Travel**: Can replay events to rebuild state

### Event Types in Our App

**Domain Events** (Past tense - something happened):
- `ticket.reserved`: Tickets were reserved
- `payment.completed`: Payment was successful
- `ticket.booked`: Booking was confirmed
- `payment.failed`: Payment failed
- `ticket.released`: Tickets were released

**Event Structure**:
```json
{
  "eventId": "uuid",           // Unique ID for idempotency
  "eventType": "ticket.reserved",
  "timestamp": "2026-03-21T10:00:00",
  "correlationId": "uuid",     // Links related events
  "version": "1.0",
  "ticketId": "...",
  "userId": "...",
  // ... event-specific data
}
```

## Patterns in This Application

### 1. SAGA Pattern

**Problem**: How to handle transactions across multiple services?

**Solution**: Coordinate through events with compensating transactions.

**Example Flow**:
```
1. User reserves ticket
   → Ticket Service saves to DB
   → Produces: ticket.reserved

2. Payment Service receives event
   → Processes payment
   → Produces: payment.completed or payment.failed

3a. Success Path (payment.completed)
   → Ticket Service receives event
   → Updates status to BOOKED
   → Produces: ticket.booked

3b. Failure Path (payment.failed)
   → Ticket Service receives event
   → Releases tickets (COMPENSATING TRANSACTION)
   → Produces: ticket.released
```

**Key Files**:
- `TicketService.java`: Orchestrates the SAGA
- `PaymentService.java`: Processes payment and publishes result
- `PaymentEventConsumer.java`: Handles payment results

**Try It**:
1. Book multiple tickets
2. ~20% will fail (simulated)
3. Watch the compensating transaction in action

### 2. Event Sourcing

**Problem**: How to maintain complete history of all changes?

**Solution**: Store every event, not just current state.

**Benefits**:
- Complete audit trail
- Can rebuild state from events
- Temporal queries ("What was the state at time X?")
- Debug issues by examining event history

**In Our App**: Audit Service
- Subscribes to ALL topics
- Stores every event in MongoDB
- Query by correlation ID to see complete flow

**Try It**:
1. Book a ticket
2. Copy the correlation ID from any event
3. Go to Audit Logs → Search by correlation ID
4. See complete flow: reserve → payment → booking

**Key Files**:
- `AuditService.java`: Stores all events
- `AuditEventConsumer.java`: Subscribes to all topics
- `AuditLog.java`: MongoDB document

### 3. Fan-Out Pattern

**Problem**: One event needs to trigger multiple actions.

**Solution**: Multiple consumers listen to the same topic.

**Example**: When `ticket.booked` event occurs:
- Notification Service → Sends confirmation email
- User Service → Updates booking history
- Audit Service → Logs the event
- All independently, all asynchronously

**Key Points**:
- Each consumer has its own consumer group
- Consumers don't know about each other
- Easy to add new consumers without changing producers

**Try It**:
1. Book a ticket
2. Check logs:
   ```bash
   docker-compose logs notification-service | grep "ticket.booked"
   docker-compose logs user-service | grep "ticket.booked"
   docker-compose logs audit-service | grep "ticket.booked"
   ```

### 4. Idempotency

**Problem**: What if we receive the same event twice?

**Solution**: Use event IDs to track processed events.

**Implementation**:
- Each event has unique `eventId`
- Consumer can check: "Have I processed this eventId before?"
- If yes, skip processing (or handle appropriately)

**In Production**:
```java
if (processedEventIds.contains(event.getEventId())) {
    log.warn("Duplicate event, skipping: {}", event.getEventId());
    return;
}
// Process event
processedEventIds.add(event.getEventId());
```

**Why Important**:
- Network issues can cause duplicate deliveries
- Kafka retries can send same message twice
- Must be safe to process same event multiple times

## Hands-On Exercises

### Exercise 1: Trace a Complete Flow

**Goal**: Understand end-to-end event flow.

1. Open Kafka UI (localhost:8080)
2. Clear existing messages or note current offsets
3. Book a ticket via UI
4. In Kafka UI, view messages in order:
   - `ticket-events` topic → see `ticket.reserved`
   - `payment-events` topic → see `payment.completed`
   - `ticket-events` topic → see `ticket.booked`
5. Copy the correlation ID
6. Find all events with that correlation ID in Audit Logs

**Questions**:
- How long did the entire flow take?
- Which partition did each message go to?
- What's the offset of each message?

### Exercise 2: Observe Consumer Groups

**Goal**: Understand how consumers share load.

1. Open Kafka UI → Consumers
2. Find "ticket-service-group"
3. See partition assignments
4. Scale the service:
   ```bash
   docker-compose up -d --scale ticket-service=3
   ```
5. Wait 30 seconds for rebalancing
6. Check Kafka UI again
7. See partitions redistributed across 3 instances

**Questions**:
- How many partitions does each instance consume?
- What happens if you scale down to 1?

### Exercise 3: Simulate Failure

**Goal**: See compensating transactions in action.

1. Book 10 tickets in quick succession
2. Watch the logs:
   ```bash
   docker-compose logs -f payment-service
   ```
3. Notice ~20% fail with "Payment declined"
4. Check the events:
   - `payment.failed` events
   - Corresponding `ticket.released` events
5. Verify in UI: Failed bookings don't appear in "My Bookings"

**Questions**:
- How does Ticket Service know payment failed?
- What happens to the reserved tickets?
- Where is this information logged?

### Exercise 4: Event Sourcing Deep Dive

**Goal**: Understand event sourcing benefits.

1. Book several tickets (mix of successes and failures)
2. Go to Audit Logs UI
3. Pick a correlation ID from a successful booking
4. Query by that correlation ID
5. See the complete flow:
   - Time of each event
   - Order of events
   - Complete event data

**Questions**:
- Could you rebuild the booking state from these events?
- What if the Ticket Service database was lost?
- How would you query "All bookings between 2-3 PM yesterday"?

### Exercise 5: Add a New Consumer

**Goal**: Experience the extensibility of EDA.

**Scenario**: Add a "Analytics Service" that tracks booking statistics.

1. Create a new service that consumes `ticket.booked` events
2. Calculate: Total bookings, revenue, popular movies
3. Deploy without changing ANY existing service
4. Watch it start receiving events immediately

**This demonstrates**: Adding features without touching existing code!

## Common Patterns Cheat Sheet

| Pattern | Use Case | Example in App |
|---------|----------|----------------|
| **SAGA** | Distributed transactions | Booking flow: reserve → pay → confirm |
| **Event Sourcing** | Complete audit trail | Audit Service stores all events |
| **Fan-Out** | One event → many actions | ticket.booked → notifications, history, audit |
| **Compensating Transaction** | Undo on failure | payment.failed → release tickets |
| **Idempotency** | Handle duplicates | Event IDs prevent duplicate processing |
| **CQRS** | Separate read/write | (Not implemented, but natural fit) |

## Best Practices

1. **Event Naming**: Past tense (ticket.reserved, not reserve.ticket)
2. **Event Data**: Include all info needed by consumers
3. **Correlation IDs**: Always link related events
4. **Versioning**: Include event version for schema evolution
5. **Idempotency**: Design for duplicate processing
6. **Error Handling**: Don't acknowledge failed messages
7. **Monitoring**: Track consumer lag
8. **Testing**: Use embedded Kafka for integration tests

## Next Steps

1. **Read the Code**: Start with event classes, then services
2. **Experiment**: Book tickets, observe events, check logs
3. **Modify**: Change payment success rate, add new events
4. **Extend**: Add new consumers, implement new patterns
5. **Learn More**: Kafka Streams, Connect, Schema Registry

## Resources

- **Kafka Documentation**: https://kafka.apache.org/documentation/
- **Confluent Blog**: https://www.confluent.io/blog/
- **Martin Fowler on Event Sourcing**: https://martinfowler.com/eaaDev/EventSourcing.html
- **Microservices.io Patterns**: https://microservices.io/patterns/

---

**Remember**: The best way to learn is by doing. Experiment, break things, observe, and learn!
