# Architecture Decision Records (ADR)

This document explains all major architectural decisions made in the Ticket Booking System, including what was chosen, why it was chosen, and why alternatives were rejected.

## Table of Contents

1. [Technology Stack Decisions](#technology-stack-decisions)
2. [Architecture Pattern Decisions](#architecture-pattern-decisions)
3. [Kafka Configuration Decisions](#kafka-configuration-decisions)
4. [Database Design Decisions](#database-design-decisions)
5. [Microservices Boundaries](#microservices-boundaries)
6. [Communication Patterns](#communication-patterns)
7. [Error Handling Strategy](#error-handling-strategy)
8. [Security Decisions](#security-decisions)
9. [Deployment Strategy](#deployment-strategy)
10. [Testing Approach](#testing-approach)

---

## Technology Stack Decisions

### ADR-001: Why Apache Kafka?

**Decision**: Use Apache Kafka as the message broker.

**Context**: Need a reliable, scalable messaging system for event-driven architecture.

**Rationale**:

**Why Kafka**:
- ✅ **Durability**: Messages persisted to disk, survives broker restarts
- ✅ **Scalability**: Handles millions of messages per second
- ✅ **Replay Capability**: Can reprocess old messages (event sourcing)
- ✅ **Ordering Guarantees**: Within partitions, maintains order
- ✅ **Industry Standard**: Widely adopted, excellent community support
- ✅ **Multiple Consumers**: Same message consumed by many services

**Why NOT RabbitMQ**:
- ❌ Messages deleted after consumption (can't replay)
- ❌ Lower throughput than Kafka
- ❌ Less suitable for event sourcing patterns
- ✅ *Better for*: Traditional message queuing, simpler setup

**Why NOT AWS SQS/SNS**:
- ❌ Cloud vendor lock-in
- ❌ Can't replay messages after consumption
- ❌ Limited ordering guarantees
- ❌ Not suitable for event sourcing
- ✅ *Better for*: Simple cloud-native apps, serverless

**Why NOT Redis Streams**:
- ❌ Not designed for high-durability use cases
- ❌ Limited ecosystem compared to Kafka
- ❌ Memory-based (more expensive at scale)
- ✅ *Better for*: Real-time analytics, caching with pub/sub

**Trade-offs Accepted**:
- More complex setup than alternatives
- Higher learning curve
- Operational overhead (Zookeeper dependency)

---

### ADR-002: Why Spring Boot?

**Decision**: Use Spring Boot for all microservices.

**Context**: Need a robust framework for building microservices quickly.

**Rationale**:

**Why Spring Boot**:
- ✅ **Excellent Kafka Integration**: Spring Kafka simplifies producer/consumer setup
- ✅ **Batteries Included**: Auto-configuration, embedded servers, dependency management
- ✅ **Production Ready**: Actuator for health checks, metrics
- ✅ **Large Ecosystem**: Spring Data JPA, Spring Data MongoDB
- ✅ **Industry Standard**: Most Java shops use Spring
- ✅ **Educational Value**: Learning Spring is valuable for career

**Why NOT Quarkus**:
- ❌ Smaller ecosystem
- ❌ Steeper learning curve
- ❌ Less familiar to beginners
- ✅ *Better for*: Native compilation, faster startup, lower memory

**Why NOT Micronaut**:
- ❌ Less mature ecosystem
- ❌ Smaller community
- ✅ *Better for*: Compile-time DI, lower memory footprint

**Why NOT Node.js/Express**:
- ❌ Weaker typing (even with TypeScript)
- ❌ Less mature Kafka clients
- ❌ Not ideal for CPU-intensive tasks
- ✅ *Better for*: I/O heavy apps, JavaScript teams

**Trade-offs Accepted**:
- Higher memory footprint than alternatives
- Slower startup time
- More verbose than some modern frameworks

---

### ADR-003: Why PostgreSQL + MongoDB (Polyglot Persistence)?

**Decision**: Use PostgreSQL for transactional data, MongoDB for audit logs.

**Context**: Different data access patterns require different storage solutions.

**Rationale**:

**PostgreSQL for Ticket/Payment/User Services**:
- ✅ **ACID Transactions**: Critical for financial data
- ✅ **Relational Data**: Users, tickets, payments are naturally relational
- ✅ **Data Integrity**: Foreign keys, constraints
- ✅ **SQL Query Power**: Complex joins, aggregations
- ✅ **Mature & Reliable**: Battle-tested in production

**MongoDB for Audit Service**:
- ✅ **Flexible Schema**: Events can have varying structures
- ✅ **Document Store**: Events are naturally documents
- ✅ **High Write Throughput**: Optimized for append-only logs
- ✅ **Horizontal Scalability**: Sharding for massive event volumes
- ✅ **No Schema Migrations**: Add new event types without migration

**Why NOT MySQL**:
- ❌ JSON support not as robust as PostgreSQL
- ❌ Less advanced features
- ✅ *Could work*: Similar to PostgreSQL for this use case

**Why NOT Single Database**:
- ❌ Relational DB not optimal for varying event schemas
- ❌ JSON columns in PostgreSQL work but less elegant
- ❌ Couples services to same database type

**Why NOT Cassandra**:
- ❌ Overkill for this scale
- ❌ Eventual consistency complexity
- ❌ Harder to learn
- ✅ *Better for*: Multi-datacenter, massive scale

**Trade-offs Accepted**:
- Operational complexity (two database types)
- Different query languages (SQL vs MongoDB query)
- More moving parts to manage

---

### ADR-004: Why Angular for Frontend?

**Decision**: Use Angular 17+ with standalone components.

**Context**: Need a modern, interactive UI for demonstrating event flows.

**Rationale**:

**Why Angular**:
- ✅ **Full Framework**: Batteries included (routing, forms, HTTP)
- ✅ **TypeScript**: Strong typing catches errors early
- ✅ **RxJS Integration**: Perfect for reactive event streams
- ✅ **Material UI**: Professional components out of the box
- ✅ **Enterprise Ready**: Used in large-scale applications
- ✅ **Standalone Components**: Modern, simpler architecture

**Why NOT React**:
- ❌ Need to choose and integrate many libraries
- ❌ More boilerplate for TypeScript setup
- ❌ JSX learning curve
- ✅ *Better for*: Component reusability, virtual DOM performance

**Why NOT Vue.js**:
- ❌ Smaller ecosystem
- ❌ Less enterprise adoption
- ✅ *Better for*: Simpler apps, gradual adoption

**Why NOT Server-Side Rendering (Next.js/Nuxt)**:
- ❌ Unnecessary complexity for this use case
- ❌ This is a SPA, not content-heavy site
- ✅ *Better for*: SEO-critical sites, content-heavy apps

**Trade-offs Accepted**:
- Steeper learning curve than alternatives
- Larger bundle size
- More opinionated (less flexibility)

---

## Architecture Pattern Decisions

### ADR-005: Why Event-Driven Architecture?

**Decision**: Use Event-Driven Architecture (EDA) instead of synchronous REST calls between services.

**Context**: Microservices need to coordinate while remaining loosely coupled.

**Rationale**:

**Why EDA**:
- ✅ **Loose Coupling**: Services don't know about each other
- ✅ **Temporal Decoupling**: Producer doesn't wait for consumers
- ✅ **Scalability**: Easy to add new consumers without changing producers
- ✅ **Resilience**: System works even if some services are down
- ✅ **Audit Trail**: All events recorded automatically
- ✅ **Event Replay**: Can reprocess events for debugging/recovery
- ✅ **Educational**: Demonstrates modern architecture patterns

**Why NOT Synchronous REST**:
```java
// Synchronous (tightly coupled)
PaymentResponse payment = paymentService.processPayment(ticket);
if (payment.isSuccessful()) {
    notificationService.sendConfirmation(ticket);
    userService.updateHistory(ticket);
}
// Problem: If any service is down, entire flow fails
```
- ❌ Tight coupling between services
- ❌ Cascading failures
- ❌ Harder to scale independently
- ❌ No built-in retry mechanism
- ❌ No audit trail
- ✅ *Better for*: Simple CRUD operations, low-latency requirements

**Why NOT Synchronous with Circuit Breakers**:
- ❌ Still tightly coupled
- ❌ Complex retry logic needed
- ❌ No natural audit trail
- ✅ *Better for*: Mixed sync/async scenarios

**Why NOT Service Mesh (Istio/Linkerd)**:
- ❌ Adds complexity for this scale
- ❌ Doesn't solve temporal coupling
- ❌ Steep learning curve
- ✅ *Better for*: Large-scale deployments, advanced traffic management
- 📝 *Note*: Can be used WITH event-driven architecture

**Trade-offs Accepted**:
- Eventual consistency (not immediate)
- More complex debugging (distributed tracing needed)
- Harder to understand for beginners
- No immediate response to user

---

### ADR-006: Why SAGA Pattern (Orchestration)?

**Decision**: Use SAGA pattern with orchestration for distributed transactions.

**Context**: Booking flow spans multiple services (Ticket → Payment → Notification).

**Rationale**:

**Why SAGA**:
- ✅ **No Distributed Transactions**: 2PC not needed
- ✅ **Compensating Transactions**: Can undo on failure
- ✅ **Service Autonomy**: Each service manages its own data
- ✅ **Scalable**: No locks across services
- ✅ **Clear Failure Handling**: Explicit compensation logic

**Why Orchestration (vs Choreography)**:
```
Orchestration (chosen):
Ticket Service → ticket.reserved → Payment Service
Payment Service → payment.completed → Ticket Service
Ticket Service → ticket.booked → (everyone)

Choreography (not chosen):
Ticket Service → ticket.reserved
Payment Service (listens) → payment.completed
Ticket Service (listens) → ticket.booked
User Service (listens) → updates history
(Each service knows what to do next)
```

**Orchestration Benefits**:
- ✅ **Centralized Control**: Ticket Service orchestrates flow
- ✅ **Easier to Understand**: Flow is explicit
- ✅ **Simpler Debugging**: Clear owner of saga
- ✅ **Better for Learning**: Easier to trace

**Choreography Trade-offs**:
- ❌ Harder to understand complete flow
- ❌ No single place to see saga logic
- ❌ Harder to add steps to flow
- ✅ *Better for*: Very loosely coupled services, simple flows

**Why NOT Two-Phase Commit (2PC)**:
```java
// 2PC approach
transaction.begin();
ticketDB.reserve(ticket);
paymentDB.charge(payment);
userDB.updateHistory(user);
transaction.commit(); // All or nothing
```
- ❌ Requires distributed transaction coordinator
- ❌ Locks resources across services
- ❌ Doesn't scale well
- ❌ Single point of failure
- ❌ Slower (blocking)
- ✅ *Better for*: Monolithic applications, small scale

**Trade-offs Accepted**:
- Eventual consistency (not immediate)
- More complex code (compensating transactions)
- Potential for inconsistency windows

---

### ADR-007: Why Event Sourcing for Audit Service?

**Decision**: Store all events in MongoDB for complete audit trail.

**Context**: Need compliance, debugging, and event replay capabilities.

**Rationale**:

**Why Event Sourcing**:
- ✅ **Complete History**: Every state change recorded
- ✅ **Audit Compliance**: Meet regulatory requirements
- ✅ **Time Travel**: Query state at any point in time
- ✅ **Event Replay**: Rebuild state from events
- ✅ **Debugging**: Trace issues through event history
- ✅ **Analytics**: Rich data for business insights

**Why Store ALL Events**:
```java
// Every event stored
ticket.reserved → stored
payment.completed → stored
ticket.booked → stored
notification.sent → stored
```
- ✅ Complete picture of what happened
- ✅ Can correlate events across services
- ✅ Never lose information

**Why NOT Traditional Audit Logs**:
```java
// Traditional approach
auditLog.info("User {} booked ticket {}", userId, ticketId);
```
- ❌ Unstructured data
- ❌ Hard to query
- ❌ Can't replay
- ❌ No event correlation

**Why NOT Database Triggers**:
```sql
-- Trigger approach
CREATE TRIGGER audit_ticket_changes
AFTER UPDATE ON tickets
FOR EACH ROW
  INSERT INTO audit_log VALUES (OLD.*, NEW.*);
```
- ❌ Couples audit to database
- ❌ Doesn't capture events from other services
- ❌ Can't see cross-service flows
- ❌ Database-specific

**Why NOT Just Keep Kafka Messages**:
- ❌ Kafka has retention limits
- ❌ Not designed for long-term storage
- ❌ Can't query efficiently
- ✅ *Could work*: With Kafka compaction, but less flexible

**Trade-offs Accepted**:
- Storage costs (every event stored forever)
- Query complexity (NoSQL vs SQL)
- Potential for massive data growth

---

### ADR-008: Why Manual Acknowledgment?

**Decision**: Use manual acknowledgment for Kafka consumers instead of auto-commit.

**Context**: Need precise control over when messages are considered "processed".

**Rationale**:

**Why Manual Acknowledgment**:
```java
@KafkaListener(topics = "payment-events")
public void handlePayment(PaymentEvent event, Acknowledgment ack) {
    try {
        processPayment(event);
        saveToDatabase(event);
        ack.acknowledge();  // Only after successful processing
    } catch (Exception e) {
        // Don't acknowledge - will retry
    }
}
```
- ✅ **At-Least-Once Delivery**: Guaranteed processing
- ✅ **Control**: Acknowledge only after DB commit
- ✅ **Reliability**: Message not lost on failure
- ✅ **Retry Logic**: Failed messages automatically retried

**Why NOT Auto-Commit**:
```java
// Auto-commit (not chosen)
@KafkaListener(topics = "payment-events")
public void handlePayment(PaymentEvent event) {
    // Offset committed immediately (before processing)
    processPayment(event);  // If this fails, message is lost!
}
```
- ❌ Message considered processed before actual processing
- ❌ Can lose messages on failure
- ❌ No control over when to commit

**Why NOT At-Most-Once**:
- ❌ Messages can be lost
- ❌ Not suitable for financial transactions
- ✅ *Better for*: Non-critical metrics, logs

**Why NOT Exactly-Once (Transactional)**:
```java
// Exactly-once approach
@Transactional
public void handlePayment(PaymentEvent event) {
    processPayment(event);
    // Kafka transaction + DB transaction together
}
```
- ❌ More complex setup
- ❌ Performance overhead
- ❌ Overkill for this use case
- ✅ *Better for*: Critical financial systems, no tolerance for duplicates
- 📝 *Note*: Idempotency handles duplicates in our approach

**Trade-offs Accepted**:
- Possible duplicate processing (need idempotency)
- Slightly more complex code
- Consumer must handle acknowledgment

---

## Kafka Configuration Decisions

### ADR-009: Why Multiple Topics Instead of Single Topic?

**Decision**: Use separate topics for different event types (ticket-events, payment-events, etc.).

**Context**: Could put all events in one topic or separate by domain.

**Rationale**:

**Why Multiple Topics**:
```
Chosen approach:
ticket-events (ticket.reserved, ticket.booked, ticket.released)
payment-events (payment.completed, payment.failed)
user-events (user.registered)
```
- ✅ **Clear Separation**: Domain boundaries explicit
- ✅ **Independent Scaling**: Scale topics independently
- ✅ **Selective Consumption**: Consumers subscribe to what they need
- ✅ **Different Retention**: Can set different retention per topic
- ✅ **Access Control**: Different permissions per topic
- ✅ **Performance**: Smaller topics, faster queries

**Why NOT Single Topic**:
```
Alternative (rejected):
all-events (ticket.reserved, payment.completed, user.registered, ...)
```
- ❌ Consumers must filter events
- ❌ Can't scale independently
- ❌ All-or-nothing retention policy
- ❌ Less clear domain boundaries
- ✅ *Better for*: Very simple systems, low event volume

**Why NOT Topic Per Event Type**:
```
Alternative (rejected):
ticket-reserved-events
ticket-booked-events
payment-completed-events
payment-failed-events
...
```
- ❌ Too many topics
- ❌ Operational overhead
- ❌ Related events split apart
- ✅ *Better for*: Very high volume, need extreme granularity

**Trade-offs Accepted**:
- More topics to manage
- Need to subscribe to multiple topics
- Slightly more complex setup

---

### ADR-010: Why 3 Partitions for Main Topics?

**Decision**: Use 3 partitions for ticket-events and payment-events, 1 for audit-events.

**Context**: Partitions enable parallelism but add complexity.

**Rationale**:

**Why 3 Partitions**:
```
ticket-events: 3 partitions
payment-events: 3 partitions
notification-events: 2 partitions
user-events: 2 partitions
audit-events: 1 partition
```

**For High-Volume Topics (3 partitions)**:
- ✅ **Parallelism**: 3 consumers can process simultaneously
- ✅ **Scalability**: Can scale to 3 instances per service
- ✅ **Performance**: Load distributed
- ✅ **Not Too Many**: Easier to manage than 10+ partitions

**For Audit Events (1 partition)**:
- ✅ **Global Ordering**: All events in order
- ✅ **Simplicity**: No partition coordination needed
- ✅ **Sufficient**: One consumer handles audit load

**Why NOT Single Partition**:
- ❌ No parallelism
- ❌ Can't scale horizontally
- ❌ Single point of bottleneck
- ✅ *Better for*: Strict global ordering needed

**Why NOT Many Partitions (10+)**:
```
// Not chosen
ticket-events: 20 partitions
```
- ❌ Operational overhead
- ❌ More rebalancing complexity
- ❌ Overkill for this volume
- ❌ More memory overhead
- ✅ *Better for*: Very high throughput (millions of messages/sec)

**Partition Key Strategy**:
```java
// Use entity ID as key
kafkaTemplate.send(topic, ticketId, event);
// Ensures all events for same ticket go to same partition
// Maintains ordering per ticket
```

**Trade-offs Accepted**:
- No global ordering across partitions
- Rebalancing complexity
- Must handle partition keys correctly

---

### ADR-011: Why JSON Serialization Instead of Avro?

**Decision**: Use JSON for event serialization instead of Avro or Protobuf.

**Context**: Need to serialize events for Kafka transmission.

**Rationale**:

**Why JSON**:
```java
// JSON serialization (chosen)
value-serializer: JsonSerializer
value-deserializer: JsonDeserializer
```
- ✅ **Human Readable**: Easy to debug
- ✅ **Flexible**: No schema registry needed
- ✅ **Simple Setup**: Works out of the box
- ✅ **Browser Compatible**: Frontend can consume easily
- ✅ **Learning**: Easier for beginners
- ✅ **Kafka UI**: Displays messages beautifully

**Why NOT Avro**:
```java
// Avro approach (rejected for now)
value-serializer: KafkaAvroSerializer
// Requires Schema Registry
```
- ❌ Requires Schema Registry (more infrastructure)
- ❌ Steeper learning curve
- ❌ Not human-readable in Kafka UI
- ❌ More complex setup
- ✅ *Better for*: Large-scale production, strict schema evolution
- ✅ **Advantages**: Smaller size, schema validation, backward compatibility

**Why NOT Protobuf**:
- ❌ Requires .proto files
- ❌ Not human-readable
- ❌ Extra compilation step
- ✅ *Better for*: Polyglot systems, performance-critical

**Why NOT String/Byte Array**:
- ❌ No structure
- ❌ Manual parsing
- ❌ Error-prone
- ✅ *Better for*: Simple messages, extreme performance

**Trade-offs Accepted**:
- Larger message size (JSON is verbose)
- No schema validation
- Manual version handling
- Slower serialization than binary formats

**Future Enhancement**:
```yaml
# Could add Schema Registry later
# For production, consider:
# - Avro for schema validation
# - Schema Registry for evolution
# - Backward/forward compatibility
```

---

## Database Design Decisions

### ADR-012: Why JPA/Hibernate Instead of Plain JDBC?

**Decision**: Use Spring Data JPA with Hibernate for relational databases.

**Context**: Need to interact with PostgreSQL databases.

**Rationale**:

**Why JPA/Hibernate**:
```java
// JPA approach (chosen)
@Entity
public class Ticket {
    @Id
    private String id;
    // ... fields
}

@Repository
interface TicketRepository extends JpaRepository<Ticket, String> {
    List<Ticket> findByUserId(String userId);
}
```
- ✅ **Productivity**: Less boilerplate
- ✅ **Type Safety**: Compile-time checking
- ✅ **Query Methods**: Derived from method names
- ✅ **Transaction Management**: Built-in
- ✅ **Lazy Loading**: Optimize queries
- ✅ **Spring Integration**: Seamless

**Why NOT Plain JDBC**:
```java
// Plain JDBC (rejected)
String sql = "SELECT * FROM tickets WHERE user_id = ?";
PreparedStatement stmt = connection.prepareStatement(sql);
stmt.setString(1, userId);
ResultSet rs = stmt.executeQuery();
// Manual mapping...
```
- ❌ Lots of boilerplate
- ❌ SQL injection risk if not careful
- ❌ Manual mapping to objects
- ❌ Manual transaction handling
- ✅ *Better for*: Complex queries, extreme performance tuning

**Why NOT jOOQ**:
- ❌ Less familiar to beginners
- ❌ Generates code from database
- ❌ More setup
- ✅ *Better for*: Complex SQL, type-safe queries

**Why NOT MyBatis**:
- ❌ XML configuration
- ❌ Less Spring integration
- ✅ *Better for*: Stored procedures, complex mappings

**Trade-offs Accepted**:
- "Magic" of ORM (generated SQL)
- N+1 query problems if not careful
- Hibernate-specific quirks
- Memory overhead

---

### ADR-013: Why Database-Per-Service?

**Decision**: Each service has its own database (ticketdb, paymentdb, userdb).

**Context**: Following microservices best practices.

**Rationale**:

**Why Separate Databases**:
```
Ticket Service → ticketdb (PostgreSQL)
Payment Service → paymentdb (PostgreSQL)
User Service → userdb (PostgreSQL)
Audit Service → auditdb (MongoDB)
```
- ✅ **Service Autonomy**: Each service owns its data
- ✅ **Independent Scaling**: Scale databases independently
- ✅ **Technology Choice**: Can choose best DB for each service
- ✅ **Loose Coupling**: No foreign keys across services
- ✅ **Independent Deployment**: Schema changes don't affect others
- ✅ **Fault Isolation**: One DB failure doesn't crash all services

**Why NOT Shared Database**:
```sql
-- Shared database approach (rejected)
tickets (belongs to Ticket Service)
payments (belongs to Payment Service)
users (belongs to User Service)
-- All in same database
```
- ❌ Tight coupling
- ❌ Can't scale independently
- ❌ Schema changes affect all services
- ❌ Violates microservices principles
- ✅ *Better for*: Monoliths, small applications

**Why NOT One Giant Service**:
- ❌ Not learning microservices
- ❌ Can't scale parts independently
- ✅ *Better for*: Simple applications, small teams

**Trade-offs Accepted**:
- Can't use foreign keys across services
- Can't use SQL joins across services
- Data consistency requires events (eventual consistency)
- More databases to manage

---

## Microservices Boundaries

### ADR-014: Why These Specific Service Boundaries?

**Decision**: Split into Ticket, Payment, Notification, User, and Audit services.

**Context**: Determining microservices boundaries is critical for success.

**Rationale**:

**Service Boundaries Chosen**:

**1. Ticket Service** (Core Domain):
```
Responsibilities:
- Manage ticket inventory
- Handle reservations
- Confirm/release bookings
- Orchestrate SAGA
```
- ✅ Single Responsibility: Ticket lifecycle
- ✅ High Cohesion: Related operations together
- ✅ Clear Boundary: Owns ticket data

**2. Payment Service** (Supporting Domain):
```
Responsibilities:
- Process payments
- Handle refunds
- Simulate payment gateway
```
- ✅ Separate Concern: Payment is distinct from ticketing
- ✅ Could Swap: Replace with real payment gateway
- ✅ Clear Boundary: Owns payment data

**3. Notification Service** (Generic Subdomain):
```
Responsibilities:
- Send emails
- Send SMS
- Send push notifications
```
- ✅ Cross-Cutting Concern: Used by all services
- ✅ Stateless: No database needed
- ✅ Easy to Replace: Could use external service

**4. User Service** (Core Domain):
```
Responsibilities:
- Manage user profiles
- Track booking history
- User preferences
```
- ✅ Clear Boundary: Owns user data
- ✅ Independent Scaling: User operations separate from tickets

**5. Audit Service** (Generic Subdomain):
```
Responsibilities:
- Store all events
- Provide audit trail
- Event sourcing
```
- ✅ Cross-Cutting: All services produce events
- ✅ Different Storage: MongoDB for events
- ✅ Read-Only: Doesn't affect main flow

**Why NOT Merge Ticket + User**:
- ❌ Violates Single Responsibility
- ❌ Couples two domains
- ❌ Harder to scale independently

**Why NOT Separate Reservation + Booking Services**:
- ❌ Too granular
- ❌ More network calls
- ❌ Harder to manage SAGA
- ✅ *Better for*: Extremely high volume where separation needed

**Why NOT API Gateway Service**:
- ❌ Adds complexity for this scale
- ❌ Frontend can call services directly
- ❌ Not needed for learning example
- ✅ *Better for*: Production systems, security, rate limiting

**Trade-offs Accepted**:
- More services to deploy/monitor
- More network communication
- More complex orchestration

---

## Communication Patterns

### ADR-015: Why Asynchronous Events, Not Synchronous REST?

**Decision**: Services communicate primarily via Kafka events, not REST calls.

**Context**: Services need to coordinate booking flow.

**Rationale**:

**Why Asynchronous Events**:
```java
// Asynchronous (chosen)
// Ticket Service
ticketRepository.save(ticket);
eventProducer.publishTicketReserved(event);
// Returns immediately

// Payment Service (listens)
@KafkaListener(topics = "ticket-events")
public void handleReservation(TicketReservedEvent event) {
    processPayment(event);
}
```
- ✅ **Loose Coupling**: Services don't call each other
- ✅ **Resilience**: Works even if consumer is down
- ✅ **Scalability**: Easy to add more consumers
- ✅ **Audit Trail**: Events logged automatically
- ✅ **Retry Built-In**: Kafka handles retries

**Why NOT Synchronous REST**:
```java
// Synchronous (rejected)
// Ticket Service
ticketRepository.save(ticket);
PaymentResponse response = restTemplate.postForObject(
    "http://payment-service/api/payments",
    paymentRequest,
    PaymentResponse.class
);
if (response.isSuccess()) {
    // Continue...
}
```
- ❌ Tight coupling
- ❌ Cascading failures (if payment service down, ticket service fails)
- ❌ No built-in retry
- ❌ Timeouts difficult to handle
- ❌ No audit trail
- ✅ *Better for*: Query operations, low-latency requirements

**When REST IS Used**:
```java
// REST for queries (appropriate)
@GetMapping("/tickets/{id}")
public Ticket getTicket(@PathVariable String id) {
    return ticketService.findById(id);
}
```
- ✅ Frontend to backend (queries)
- ✅ Read operations
- ✅ Immediate response needed

**Why NOT gRPC**:
- ❌ Still synchronous
- ❌ Tight coupling
- ❌ More complex setup
- ✅ *Better for*: High-performance sync calls, polyglot systems

**Trade-offs Accepted**:
- Eventual consistency
- No immediate response
- More complex debugging

---

### ADR-016: Why No Service-to-Service REST Calls?

**Decision**: Backend services do NOT make REST calls to each other.

**Context**: Could combine events + REST for service communication.

**Rationale**:

**Why No Service-to-Service REST**:
```
Chosen:
Service A → Kafka → Service B (always)

Rejected:
Service A → REST → Service B (never)
```
- ✅ **Consistency**: One communication pattern
- ✅ **Simplicity**: Easier to understand
- ✅ **Audit Trail**: All interactions logged
- ✅ **Resilience**: No cascading failures
- ✅ **Educational**: Pure event-driven architecture

**Why NOT Hybrid Approach**:
```
Alternative (rejected):
- Events for commands (ticket.reserve)
- REST for queries (getTicketStatus)
```
- ❌ Two patterns to learn
- ❌ When to use which?
- ❌ More complexity
- ✅ *Better for*: Production systems with low-latency requirements

**Exception: Frontend to Backend**:
```
Frontend → REST → Backend Services (OK)
- User needs immediate response
- REST/HTTP is standard for web
```

**Trade-offs Accepted**:
- Can't do real-time queries between services
- All communication is asynchronous
- Might need to replicate data (eventual consistency)

---

## Error Handling Strategy

### ADR-017: Why 80% Payment Success Rate?

**Decision**: Simulate 20% payment failure rate.

**Context**: Need to demonstrate error handling and compensating transactions.

**Rationale**:

**Why Simulate Failures**:
```java
// PaymentService.java
boolean paymentSuccessful = random.nextDouble() < 0.8; // 80% success
```
- ✅ **Learning**: See compensating transactions in action
- ✅ **Realistic**: Real systems fail sometimes
- ✅ **Testing**: Forces error handling paths
- ✅ **Observability**: Can watch failure flows
- ✅ **Confidence**: Proves system handles failures gracefully

**Why 20% Failure Rate** (not 5% or 50%):
- 5% → Too rare, might not see failures
- 20% → Frequent enough to see regularly
- 50% → Unrealistic, annoying for testing

**Why NOT Always Succeed**:
```java
// Always success (rejected)
boolean paymentSuccessful = true;
```
- ❌ Doesn't test error paths
- ❌ Doesn't show compensating transactions
- ❌ False confidence in system

**Why NOT Configurable Via API**:
```java
// Not implemented (could add)
@PostMapping("/payment/set-failure-rate")
public void setFailureRate(@RequestParam double rate) {
    this.failureRate = rate;
}
```
- Not needed for this learning example
- Could add if wanted more control

**Trade-offs Accepted**:
- Users experience "failures" (intentional)
- Need to explain this is simulated
- Might confuse beginners initially

---

### ADR-018: Why Manual Acknowledgment + Retry (Not DLQ Yet)?

**Decision**: Use manual acknowledgment with automatic retries, no Dead Letter Queue initially.

**Context**: Need to handle failed message processing.

**Rationale**:

**Current Approach**:
```java
@KafkaListener(topics = "payment-events")
public void handlePayment(Event event, Acknowledgment ack) {
    try {
        process(event);
        ack.acknowledge();  // Success
    } catch (Exception e) {
        log.error("Failed", e);
        // Don't acknowledge - Kafka will retry
    }
}
```
- ✅ **Simple**: No extra infrastructure
- ✅ **Automatic Retries**: Kafka handles it
- ✅ **Learning**: Focus on core concepts first

**Why NOT Dead Letter Queue (Yet)**:
```java
// DLQ approach (not implemented yet)
try {
    process(event);
} catch (Exception e) {
    if (retryCount > MAX_RETRIES) {
        sendToDLQ(event);  // Poison message
    }
}
```
- ❌ Adds complexity
- ❌ More infrastructure
- ❌ Not critical for learning example
- ✅ *Better for*: Production systems
- ✅ **Future Enhancement**: Should be added

**Why NOT Ignore Failures**:
```java
// Bad approach (never do this)
try {
    process(event);
} catch (Exception e) {
    log.error("Failed", e);
    // Just acknowledge anyway
    ack.acknowledge();  // ❌ Message lost!
}
```
- ❌ Loses messages
- ❌ Data inconsistency
- ❌ Never acceptable

**Trade-offs Accepted**:
- Poison messages can block consumption
- No visibility into permanently failed messages
- Manual intervention needed for stuck messages

**Recommended Addition** (for production):
```java
// Add DLQ handling
private static final int MAX_RETRIES = 3;
private Map<String, Integer> retries = new ConcurrentHashMap<>();

@KafkaListener(topics = "payment-events")
public void handlePayment(Event event, Acknowledgment ack) {
    try {
        process(event);
        retries.remove(event.getEventId());
        ack.acknowledge();
    } catch (Exception e) {
        int count = retries.merge(event.getEventId(), 1, Integer::sum);
        if (count > MAX_RETRIES) {
            dlqProducer.send("dead-letter-queue", event);
            retries.remove(event.getEventId());
            ack.acknowledge();
        }
        // Else: don't acknowledge, will retry
    }
}
```

---

## Security Decisions

### ADR-019: Why No Authentication/Authorization?

**Decision**: No authentication or authorization implemented.

**Context**: This is a learning example, not production system.

**Rationale**:

**Why No Auth**:
- ✅ **Focus**: Keep focus on Kafka and EDA
- ✅ **Simplicity**: Easier to test and demo
- ✅ **Learning**: One concept at a time
- ✅ **Scope**: Auth is separate concern

**What's Missing** (for production):
```yaml
# Would need:
- Spring Security
- JWT tokens
- User authentication
- Role-based access control
- OAuth2/OIDC
- API keys for service-to-service
```

**Why NOT Add Auth**:
- ❌ Distracts from core learning (Kafka)
- ❌ Adds complexity
- ❌ Makes testing harder
- ✅ *Should add for*: Production deployment

**Trade-offs Accepted**:
- Anyone can call any API
- No user context in events
- Not production-ready from security perspective

**Easy to Add Later**:
```java
// Add Spring Security
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").authenticated()
            )
            .oauth2ResourceServer(OAuth2ResourceServerConfigurer::jwt)
            .build();
    }
}
```

---

### ADR-020: Why CORS Enabled for All Origins?

**Decision**: Enable CORS for all origins (`*`).

**Context**: Frontend needs to call backend APIs.

**Rationale**:

**Why Allow All**:
```java
@CrossOrigin(origins = "*")
@RestController
public class TicketController {
    // ...
}
```
- ✅ **Development**: Easy local testing
- ✅ **Demo**: Works from any origin
- ✅ **Learning**: No CORS issues to debug

**Why NOT Restrict Origins**:
```java
// Production approach (not used)
@CrossOrigin(origins = "https://myapp.com")
```
- ❌ Harder for learning
- ❌ Need to configure for each environment
- ✅ *Better for*: Production deployment

**Trade-offs Accepted**:
- Less secure
- Anyone can call APIs from browser
- Not production-ready

**Production Recommendation**:
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOrigins("https://myapp.com")
            .allowedMethods("GET", "POST", "PUT", "DELETE")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}
```

---

## Deployment Strategy

### ADR-021: Why Docker Compose (Not Kubernetes)?

**Decision**: Use Docker Compose for deployment.

**Context**: Need container orchestration for local development and learning.

**Rationale**:

**Why Docker Compose**:
```yaml
# docker-compose.yml
services:
  kafka:
    image: confluentinc/cp-kafka:7.5.0
    # ...
  ticket-service:
    build: ./ticket-service
    # ...
```
- ✅ **Simple**: Single command to start (`docker-compose up`)
- ✅ **Local Development**: Runs on laptop
- ✅ **Learning**: Easy to understand
- ✅ **Fast**: Quick startup
- ✅ **Debugging**: Easy to view logs
- ✅ **No Cost**: No cloud infrastructure needed

**Why NOT Kubernetes**:
```yaml
# Would need:
- Kubernetes cluster
- Helm charts
- Ingress controller
- Persistent volumes
- Services, Deployments
- ConfigMaps, Secrets
```
- ❌ Much more complex
- ❌ Steep learning curve
- ❌ Overkill for local development
- ❌ Requires cluster (local or cloud)
- ✅ *Better for*: Production, multi-cluster, auto-scaling

**Why NOT Docker Swarm**:
- ❌ Less popular than Compose or K8s
- ❌ Fewer features than K8s
- ❌ Similar complexity to Compose
- ✅ *Better for*: Simple production deployments

**Why NOT Local Processes**:
```bash
# Not chosen
java -jar ticket-service.jar &
java -jar payment-service.jar &
# ... etc
```
- ❌ Hard to manage
- ❌ Port conflicts
- ❌ No isolation
- ❌ Environment differences

**Trade-offs Accepted**:
- Not production-grade
- No auto-scaling
- No self-healing
- Single host only

**Production Path**:
```
Docker Compose (learning)
    ↓
Docker Compose + Swarm (simple production)
    ↓
Kubernetes (enterprise production)
```

---

### ADR-022: Why Multi-Stage Docker Builds?

**Decision**: Use multi-stage Dockerfile builds.

**Context**: Need to build and package Spring Boot applications.

**Rationale**:

**Why Multi-Stage**:
```dockerfile
# Stage 1: Build
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY common common/
COPY ticket-service ticket-service/
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/ticket-service/target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
```
- ✅ **Smaller Images**: Only runtime artifacts
- ✅ **Faster Builds**: Caches dependencies
- ✅ **Security**: No build tools in runtime image
- ✅ **Best Practice**: Industry standard

**Size Comparison**:
- Single stage: ~500MB (includes Maven, sources)
- Multi-stage: ~200MB (only JRE + JAR)

**Why NOT Single Stage**:
```dockerfile
# Single stage (rejected)
FROM maven:3.9-eclipse-temurin-17
COPY . .
RUN mvn package
ENTRYPOINT ["java", "-jar", "target/app.jar"]
```
- ❌ Huge image size
- ❌ Security risk (build tools in production)
- ❌ Slower startup
- ❌ Contains source code

**Why NOT Pre-Built JARs**:
- ❌ Need to build locally first
- ❌ Platform-specific
- ❌ Manual process

**Trade-offs Accepted**:
- Slightly more complex Dockerfile
- Longer initial build time

---

## Testing Approach

### ADR-023: Why No Comprehensive Tests?

**Decision**: Minimal tests, focus on demonstrating functionality.

**Context**: This is a learning project, not production code.

**Rationale**:

**Why Minimal Tests**:
- ✅ **Focus**: Emphasis on Kafka concepts
- ✅ **Time**: Comprehensive tests take significant effort
- ✅ **Learning**: Tests can be added as exercise
- ✅ **Demo**: Manual testing via UI is sufficient

**What's Missing**:
```java
// Would add:
- Unit tests for services
- Integration tests with @EmbeddedKafka
- Contract tests between services
- E2E tests with Testcontainers
- Performance tests
- Chaos engineering tests
```

**Why NOT Add All Tests**:
- ❌ Distracts from learning Kafka
- ❌ Significant additional code
- ❌ Maintenance burden
- ✅ *Should add for*: Production systems

**Testing Strategy If Added**:
```java
// Unit tests
@Test
void reserveTicket_shouldPublishEvent() {
    // Test service logic
}

// Integration tests
@SpringBootTest
@EmbeddedKafka
class TicketServiceIntegrationTest {
    @Test
    void fullBookingFlow_shouldComplete() {
        // Test event flow
    }
}

// E2E tests
@Testcontainers
class BookingE2ETest {
    @Container
    static KafkaContainer kafka = new KafkaContainer();

    @Test
    void completeBooking_endToEnd() {
        // Test entire system
    }
}
```

**Trade-offs Accepted**:
- No safety net for refactoring
- Bugs may go unnoticed
- No regression protection
- Manual testing required

---

## Summary

### Key Architectural Principles

1. **Learning First**: Optimize for understanding, not production perfection
2. **Event-Driven**: Pure EDA pattern for loose coupling
3. **Polyglot Persistence**: Right database for right job
4. **Observability**: Everything logged and traceable
5. **Failure Handling**: Explicit compensating transactions
6. **Simplicity**: Avoid unnecessary complexity

### For Production, Add:

- ✅ Authentication & Authorization
- ✅ Dead Letter Queue handling
- ✅ Comprehensive tests
- ✅ Distributed tracing (Zipkin/Jaeger)
- ✅ Circuit breakers
- ✅ Rate limiting
- ✅ Schema Registry (Avro)
- ✅ Kubernetes deployment
- ✅ Monitoring & alerting
- ✅ Backup & disaster recovery

### Decision Framework

When faced with architectural choices:

1. **Learning Value**: Does it teach core concepts?
2. **Complexity**: Is it necessary or overkill?
3. **Industry Standards**: Is it commonly used?
4. **Future-Proof**: Can we add features later?
5. **Debuggability**: Can we understand what's happening?

---

This document should be updated as new decisions are made or existing ones are revised.
