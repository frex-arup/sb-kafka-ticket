# 🎬 Ticket Booking System - Learn Kafka & Event-Driven Architecture

A comprehensive, production-ready example of Event-Driven Architecture (EDA) using Apache Kafka, Spring Boot microservices, and Angular. Built specifically for learning Kafka concepts through hands-on experience.

## 🎯 What You'll Learn

- **SAGA Pattern**: Distributed transactions across microservices
- **Event Sourcing**: Complete audit trail of all state changes
- **Compensating Transactions**: How to handle failures gracefully
- **Fan-out Pattern**: One event triggering multiple consumers
- **Kafka Fundamentals**: Topics, partitions, producers, consumers, consumer groups
- **Idempotency**: Handling duplicate messages
- **Event-Driven Communication**: Services coordinating via events, not REST calls

## 🏗️ Architecture

### Microservices (5 backend + 1 frontend)

1. **Ticket Service** (Port 8081)
   - Manages ticket reservations and bookings
   - Produces: `ticket.reserved`, `ticket.booked`, `ticket.released`
   - Consumes: `payment.completed`, `payment.failed`

2. **Payment Service** (Port 8082)
   - Processes payments (80% success rate for demo)
   - Produces: `payment.completed`, `payment.failed`
   - Consumes: `ticket.reserved`

3. **Notification Service** (Port 8083)
   - Sends notifications (simulated)
   - Pure consumer (fan-out pattern)
   - Consumes: All booking-related events

4. **User Service** (Port 8084)
   - Manages user profiles and booking history
   - Produces: `user.registered`
   - Consumes: `ticket.booked`, `ticket.released`

5. **Audit Service** (Port 8085)
   - Event sourcing and complete audit trail
   - Stores ALL events in MongoDB
   - Consumes: ALL events from all topics

6. **Angular Frontend** (Port 4200)
   - Interactive UI for all operations
   - Real-time event monitoring
   - Audit log visualization

### Infrastructure

- **Apache Kafka** (Port 9092): Message broker
- **Zookeeper** (Port 2181): Kafka coordination
- **Kafka UI** (Port 8080): Web interface for Kafka
- **PostgreSQL** (Port 5432): Databases for services
- **MongoDB** (Port 27017): Event store for audit service

## 🚀 Quick Start

### Prerequisites

- Docker & Docker Compose
- Java 17+ (for local development)
- Node.js 20+ (for frontend development)
- Maven 3.9+ (for building)

### Run the Complete System

```bash
# Start all services
docker-compose up -d

# Wait for services to be healthy (30-60 seconds)
docker-compose ps

# Check logs
docker-compose logs -f ticket-service payment-service
```

### Access the Applications

- **Frontend UI**: http://localhost:4200
- **Kafka UI**: http://localhost:8080
- **Ticket Service API**: http://localhost:8081/swagger-ui.html
- **Payment Service API**: http://localhost:8082/swagger-ui.html
- **User Service API**: http://localhost:8084/swagger-ui.html
- **Audit Service API**: http://localhost:8085/swagger-ui.html

## 📖 Usage Guide

### 1. Book a Ticket (Happy Path)

1. Open http://localhost:4200
2. Go to "Book Tickets"
3. Fill in the form:
   - Movie: "Inception"
   - User ID: Create a test user first or use any string
   - Show Time: Select future date/time
   - Seats: "A1, A2"
   - Amount: 30
4. Click "Reserve Tickets"
5. Watch the flow:
   - Ticket reserved (status: RESERVED)
   - Payment processing...
   - Payment successful (status: BOOKED)
   - Confirmation code generated

### 2. View Events in Real-Time

1. Go to "Event Monitor" in the UI
2. Click "Refresh Events"
3. See the event flow:
   - `ticket.reserved` → `payment.completed` → `ticket.booked`
4. Each event shows:
   - Event ID (for idempotency)
   - Correlation ID (links related events)
   - Topic, partition, offset
   - Complete event data

### 3. Explore Kafka UI

1. Open http://localhost:8080
2. Navigate to "Topics"
3. View messages in each topic
4. Check consumer groups and lag
5. See partition distribution

### 4. Test Failure Scenario

1. Book multiple tickets (try 5-10 times)
2. ~20% will fail (simulated payment failure)
3. Watch the compensating transaction:
   - `payment.failed` event
   - Ticket status changes to RELEASED
   - Notification sent

### 5. Query Audit Trail

1. Go to "Audit Logs" in the UI
2. Copy a Correlation ID from an event
3. Search by Correlation ID
4. See complete flow: reservation → payment → booking
5. This demonstrates event sourcing

## 🎓 Learning Paths

### For Kafka Beginners

1. Start with the Frontend UI - book a ticket
2. Watch events flow in "Event Monitor"
3. Open Kafka UI and explore topics
4. Read the code comments in:
   - `common/src/main/java/com/ticketbooking/common/event/BaseEvent.java`
   - `ticket-service/src/main/java/com/ticketbooking/ticket/service/TicketService.java`
   - `payment-service/src/main/java/com/ticketbooking/payment/service/PaymentService.java`

### Understanding SAGA Pattern

1. Trace a booking flow from start to finish
2. Examine these files:
   - Ticket Service: How it produces `ticket.reserved` and consumes payment events
   - Payment Service: How it consumes ticket events and produces payment results
   - See the orchestration in action

### Event Sourcing Deep Dive

1. Open Audit Service code
2. See how ALL events are stored in MongoDB
3. Query events by correlation ID
4. Understand how you could rebuild state from events

## 🔧 Development

### Build Locally

```bash
# Build all services
mvn clean package

# Build specific service
mvn clean package -pl ticket-service -am

# Run locally (requires Docker for Kafka)
docker-compose up -d kafka postgres mongodb
cd ticket-service
mvn spring-boot:run
```

### Frontend Development

```bash
cd frontend-app
npm install
npm start
# Access at http://localhost:4200
```

### Run Tests

```bash
# Run all tests
mvn test

# Run specific service tests
mvn test -pl payment-service
```

## 📊 Monitoring

### Service Health

```bash
# Check all services
docker-compose ps

# View logs
docker-compose logs -f ticket-service

# Check health endpoints
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

### Kafka Monitoring

- **Kafka UI**: http://localhost:8080
  - View topics and messages
  - Monitor consumer lag
  - Check partition distribution

## 🐛 Troubleshooting

### Services Won't Start

```bash
# Check Docker resources (needs ~4GB RAM)
docker stats

# Restart services
docker-compose down
docker-compose up -d
```

### Can't Connect to Kafka

```bash
# Check Kafka health
docker-compose logs kafka

# Verify topics exist
docker-compose exec kafka kafka-topics --list --bootstrap-server localhost:9092
```

### Database Issues

```bash
# Recreate databases
docker-compose down -v  # WARNING: Deletes data
docker-compose up -d
```

## 🌟 Key Features

### For Learning

- **Well-Commented Code**: Every pattern explained
- **Real-Time Visualization**: See events flow
- **Failure Scenarios**: 20% payment failure to see error handling
- **Complete Audit Trail**: Every event recorded
- **Interactive UI**: No curl commands needed

### Production Patterns

- Idempotency handling (event IDs)
- Manual acknowledgment for control
- Error handling and retries
- Health checks and monitoring
- API documentation (Swagger)
- Multi-stage Docker builds
- Proper CORS configuration

## 📚 Additional Resources

- **Kafka Documentation**: https://kafka.apache.org/documentation/
- **Spring Kafka Guide**: https://docs.spring.io/spring-kafka/reference/
- **SAGA Pattern**: https://microservices.io/patterns/data/saga.html
- **Event Sourcing**: https://martinfowler.com/eaaDev/EventSourcing.html

## 🤝 Contributing

This is a learning project. Feel free to:
- Add more event types
- Implement additional patterns
- Add more microservices
- Improve the UI
- Add E2E tests

## 📝 License

This project is for educational purposes. Feel free to use and modify.

## 🎯 Next Steps

After mastering this example:
1. Implement Dead Letter Queue handling
2. Add Kafka Streams for event processing
3. Implement CQRS pattern
4. Add distributed tracing (Zipkin/Jaeger)
5. Implement circuit breakers
6. Add Kafka Connect for database integration

---

**Happy Learning! 🚀**

For questions or issues, check the logs and Kafka UI first. Most problems are visible there!
