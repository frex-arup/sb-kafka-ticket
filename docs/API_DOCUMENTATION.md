# API Documentation - Ticket Booking System

## Overview

This document provides complete REST API documentation for all microservices in the ticket booking system.

**Base URLs**:
- Ticket Service: `http://localhost:8081/api`
- Payment Service: `http://localhost:8082/api`
- User Service: `http://localhost:8084/api`
- Audit Service: `http://localhost:8085/api`

**Swagger UI**:
- Ticket Service: http://localhost:8081/swagger-ui.html
- Payment Service: http://localhost:8082/swagger-ui.html
- User Service: http://localhost:8084/swagger-ui.html
- Audit Service: http://localhost:8085/swagger-ui.html

---

## Ticket Service APIs

### 1. Reserve Ticket

Creates a new ticket reservation and initiates payment process.

**Endpoint**: `POST /api/tickets/reserve`

**Request Body**:
```json
{
  "movieName": "Inception",
  "showTime": "2026-03-21T19:00:00",
  "seatNumbers": ["A1", "A2"],
  "userId": "user-123",
  "totalAmount": 150.00,
  "paymentProvider": "RAZORPAY"
}
```

**Request Fields**:
| Field           | Type     | Required | Description                          |
|-----------------|----------|----------|--------------------------------------|
| movieName       | String   | Yes      | Name of the movie                    |
| showTime        | DateTime | Yes      | Show date and time (ISO 8601)        |
| seatNumbers     | Array    | Yes      | List of seat identifiers             |
| userId          | String   | Yes      | User ID (UUID or email)              |
| totalAmount     | Decimal  | Yes      | Total ticket price                   |
| paymentProvider | String   | Yes      | "RAZORPAY" or "STRIPE"               |

**Response** (201 Created):
```json
{
  "id": "ticket-uuid-123",
  "movieName": "Inception",
  "showTime": "2026-03-21T19:00:00",
  "seatNumbers": ["A1", "A2"],
  "userId": "user-123",
  "totalAmount": 150.00,
  "status": "RESERVED",
  "paymentProvider": "RAZORPAY",
  "reservedUntil": "2026-03-21T10:15:00",
  "createdAt": "2026-03-21T10:00:00"
}
```

**Status Codes**:
- `201` - Ticket reserved successfully
- `400` - Invalid request (validation errors)
- `409` - Seats already reserved
- `500` - Internal server error

**Event Published**: `TicketReservedEvent` → `ticket-events` topic

---

### 2. Get Ticket by ID

Retrieves ticket details by ticket ID.

**Endpoint**: `GET /api/tickets/{ticketId}`

**Path Parameters**:
- `ticketId` (String, required): Ticket UUID

**Response** (200 OK):
```json
{
  "id": "ticket-uuid-123",
  "movieName": "Inception",
  "showTime": "2026-03-21T19:00:00",
  "seatNumbers": ["A1", "A2"],
  "userId": "user-123",
  "totalAmount": 150.00,
  "status": "BOOKED",
  "paymentProvider": "RAZORPAY",
  "confirmationCode": "CONF-ABC123",
  "createdAt": "2026-03-21T10:00:00",
  "updatedAt": "2026-03-21T10:05:00"
}
```

**Status Codes**:
- `200` - Ticket found
- `404` - Ticket not found
- `500` - Internal server error

---

### 3. Get User's Tickets

Retrieves all tickets for a specific user.

**Endpoint**: `GET /api/tickets/user/{userId}`

**Path Parameters**:
- `userId` (String, required): User ID

**Response** (200 OK):
```json
[
  {
    "id": "ticket-uuid-123",
    "movieName": "Inception",
    "status": "BOOKED",
    "totalAmount": 150.00,
    "createdAt": "2026-03-21T10:00:00"
  },
  {
    "id": "ticket-uuid-456",
    "movieName": "Interstellar",
    "status": "RELEASED",
    "totalAmount": 150.00,
    "createdAt": "2026-03-21T09:00:00"
  }
]
```

**Status Codes**:
- `200` - Success (empty array if no tickets)
- `500` - Internal server error

---

### 4. Get All Tickets

Retrieves all tickets (admin endpoint).

**Endpoint**: `GET /api/tickets`

**Query Parameters** (optional):
- `status` (String): Filter by status (RESERVED, BOOKED, RELEASED)
- `page` (Integer): Page number (default: 0)
- `size` (Integer): Page size (default: 20)

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": "ticket-uuid-123",
      "movieName": "Inception",
      "status": "BOOKED",
      "totalAmount": 150.00
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

---

## Payment Service APIs

### 1. Get Payment Status by Ticket ID

**⭐ NEW** - Used by frontend to poll for payment URL after reservation.

**Endpoint**: `GET /api/payments/ticket/{ticketId}/status`

**Path Parameters**:
- `ticketId` (String, required): Ticket UUID

**Response** (200 OK):
```json
{
  "paymentId": "pay-uuid-789",
  "ticketId": "ticket-uuid-123",
  "status": "PENDING",
  "paymentProvider": "RAZORPAY",
  "paymentUrl": "https://rzp.io/l/xxxxxx",
  "amount": 150.00,
  "transactionId": null,
  "failureReason": null,
  "createdAt": "2026-03-21T10:00:00",
  "updatedAt": "2026-03-21T10:00:30"
}
```

**Response Fields**:
| Field           | Type     | Description                                |
|-----------------|----------|--------------------------------------------|
| paymentId       | String   | Payment UUID                               |
| ticketId        | String   | Associated ticket UUID                     |
| status          | String   | PENDING, COMPLETED, or FAILED              |
| paymentProvider | String   | RAZORPAY, STRIPE, or SIMULATED             |
| paymentUrl      | String   | Payment link URL (if available)            |
| amount          | Decimal  | Payment amount                             |
| transactionId   | String   | Gateway transaction ID (after completion)  |
| failureReason   | String   | Reason for failure (if failed)             |
| createdAt       | DateTime | Payment creation timestamp                 |
| updatedAt       | DateTime | Last update timestamp                      |

**Status Codes**:
- `200` - Payment found
- `404` - Payment not found for ticket
- `500` - Internal server error

**Usage Pattern** (Frontend):
```javascript
// Poll every 2 seconds until paymentUrl appears
const pollInterval = setInterval(() => {
  fetch(`/api/payments/ticket/${ticketId}/status`)
    .then(res => res.json())
    .then(data => {
      if (data.paymentUrl) {
        clearInterval(pollInterval);
        window.location.href = data.paymentUrl; // Redirect to payment
      }
    });
}, 2000);
```

---

### 2. Razorpay Webhook Handler

**⭐ NEW** - Receives payment status updates from Razorpay.

**Endpoint**: `POST /api/payments/webhook/razorpay`

**Headers**:
- `X-Razorpay-Signature` (required): HMAC-SHA256 signature for verification
- `Content-Type: application/json`

**Request Body** (from Razorpay):
```json
{
  "entity": "event",
  "event": "payment.captured",
  "payload": {
    "payment": {
      "entity": {
        "id": "pay_xxxxx",
        "order_id": "order_xxxxx",
        "amount": 15000,
        "status": "captured",
        "method": "card",
        "captured_at": 1677649200
      }
    }
  }
}
```

**Response**:
- `200 OK` - Webhook processed successfully
- `401 Unauthorized` - Invalid signature
- `500 Internal Server Error` - Processing failed

**Security**: Signature verification is MANDATORY
```java
boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
if (!isValid) {
    return 401; // Reject
}
```

**Event Published**: `PaymentCompletedEvent` or `PaymentFailedEvent`

---

### 3. Stripe Failure Page

**⭐ NEW** - Mock endpoint for Stripe simulation.

**Endpoint**: `GET /api/payments/stripe-failure-page/{paymentId}`

**Path Parameters**:
- `paymentId` (String, required): Payment UUID

**Response** (200 OK):
Returns HTML page with failure message.

**Purpose**: Used as mock payment URL for Stripe simulation. Always shows failure.

---

## User Service APIs

### 1. Create User

Creates a new user profile.

**Endpoint**: `POST /api/users`

**Request Body**:
```json
{
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "+1234567890"
}
```

**Response** (201 Created):
```json
{
  "id": "user-uuid-456",
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "+1234567890",
  "createdAt": "2026-03-21T10:00:00"
}
```

**Status Codes**:
- `201` - User created
- `400` - Invalid request
- `409` - Email already exists
- `500` - Internal server error

---

### 2. Get User by ID

**Endpoint**: `GET /api/users/{userId}`

**Response** (200 OK):
```json
{
  "id": "user-uuid-456",
  "name": "John Doe",
  "email": "john@example.com",
  "phone": "+1234567890",
  "createdAt": "2026-03-21T10:00:00"
}
```

**Status Codes**:
- `200` - User found
- `404` - User not found
- `500` - Internal server error

---

### 3. Get User's Bookings

Retrieves booking history for a user.

**Endpoint**: `GET /api/users/{userId}/bookings`

**Response** (200 OK):
```json
[
  {
    "ticketId": "ticket-uuid-123",
    "movieName": "Inception",
    "showTime": "2026-03-21T19:00:00",
    "seatNumbers": ["A1", "A2"],
    "amount": 150.00,
    "status": "BOOKED",
    "confirmationCode": "CONF-ABC123",
    "bookedAt": "2026-03-21T10:05:00"
  }
]
```

**Status Codes**:
- `200` - Success (empty array if no bookings)
- `404` - User not found
- `500` - Internal server error

---

## Audit Service APIs

### 1. Get All Audit Events

Retrieves audit trail events with optional filtering.

**Endpoint**: `GET /api/audit/events`

**Query Parameters**:
| Parameter      | Type     | Required | Description                    |
|----------------|----------|----------|--------------------------------|
| correlationId  | String   | No       | Filter by correlation ID       |
| eventType      | String   | No       | Filter by event type           |
| startDate      | DateTime | No       | Start of date range            |
| endDate        | DateTime | No       | End of date range              |
| page           | Integer  | No       | Page number (default: 0)       |
| size           | Integer  | No       | Page size (default: 50)        |

**Example Request**:
```
GET /api/audit/events?correlationId=corr-123&eventType=payment.completed
```

**Response** (200 OK):
```json
{
  "content": [
    {
      "id": "audit-uuid-789",
      "eventId": "evt-123",
      "eventType": "payment.completed",
      "topic": "payment-events",
      "partition": 1,
      "offset": 42,
      "timestamp": "2026-03-21T10:05:00",
      "correlationId": "corr-123",
      "eventData": {
        "paymentId": "pay-789",
        "ticketId": "ticket-123",
        "amount": 150.00,
        "transactionId": "txn-xyz"
      }
    }
  ],
  "totalElements": 5,
  "totalPages": 1,
  "number": 0,
  "size": 50
}
```

**Status Codes**:
- `200` - Success
- `400` - Invalid query parameters
- `500` - Internal server error

---

### 2. Get Audit Event by ID

**Endpoint**: `GET /api/audit/events/{auditId}`

**Response** (200 OK):
```json
{
  "id": "audit-uuid-789",
  "eventId": "evt-123",
  "eventType": "payment.completed",
  "topic": "payment-events",
  "partition": 1,
  "offset": 42,
  "timestamp": "2026-03-21T10:05:00",
  "correlationId": "corr-123",
  "eventData": {
    "paymentId": "pay-789",
    "ticketId": "ticket-123",
    "amount": 150.00,
    "transactionId": "txn-xyz"
  },
  "createdAt": "2026-03-21T10:05:01"
}
```

**Status Codes**:
- `200` - Audit event found
- `404` - Audit event not found
- `500` - Internal server error

---

## Event Payloads (Kafka Events)

### TicketReservedEvent

**Topic**: `ticket-events`

**Payload**:
```json
{
  "eventType": "ticket.reserved",
  "eventId": "evt-uuid-123",
  "timestamp": "2026-03-21T10:00:00",
  "ticketId": "ticket-uuid-456",
  "movieName": "Inception",
  "showTime": "2026-03-21T19:00:00",
  "seatNumbers": ["A1", "A2"],
  "userId": "user-uuid-789",
  "totalAmount": 150.00,
  "reservedUntil": "2026-03-21T10:15:00",
  "paymentProvider": "RAZORPAY",
  "correlationId": "corr-uuid-abc"
}
```

---

### PaymentInitiatedEvent ⭐ NEW

**Topic**: `payment-events`

**Payload**:
```json
{
  "eventType": "payment.initiated",
  "eventId": "evt-uuid-124",
  "timestamp": "2026-03-21T10:00:02",
  "paymentId": "pay-uuid-111",
  "ticketId": "ticket-uuid-456",
  "userId": "user-uuid-789",
  "paymentUrl": "https://rzp.io/l/xxxxxx",
  "paymentProvider": "RAZORPAY",
  "amount": 150.00,
  "expiresAt": "2026-03-21T10:15:00",
  "correlationId": "corr-uuid-abc"
}
```

**Purpose**: Notifies frontend that payment link is ready

---

### PaymentCompletedEvent

**Topic**: `payment-events`

**Payload**:
```json
{
  "eventType": "payment.completed",
  "eventId": "evt-uuid-125",
  "timestamp": "2026-03-21T10:05:00",
  "paymentId": "pay-uuid-111",
  "ticketId": "ticket-uuid-456",
  "amount": 150.00,
  "paymentMethod": "ONLINE",
  "userId": "user-uuid-789",
  "transactionId": "pay_xxxxx",
  "correlationId": "corr-uuid-abc"
}
```

---

### PaymentFailedEvent

**Topic**: `payment-events`

**Payload**:
```json
{
  "eventType": "payment.failed",
  "eventId": "evt-uuid-126",
  "timestamp": "2026-03-21T10:05:00",
  "paymentId": "pay-uuid-222",
  "ticketId": "ticket-uuid-789",
  "amount": 150.00,
  "userId": "user-uuid-789",
  "failureReason": "Stripe payment declined (simulated)",
  "correlationId": "corr-uuid-def"
}
```

---

### TicketBookedEvent

**Topic**: `ticket-events`

**Payload**:
```json
{
  "eventType": "ticket.booked",
  "eventId": "evt-uuid-127",
  "timestamp": "2026-03-21T10:05:01",
  "bookingId": "booking-uuid-333",
  "ticketId": "ticket-uuid-456",
  "userId": "user-uuid-789",
  "confirmationCode": "CONF-ABC123",
  "correlationId": "corr-uuid-abc"
}
```

---

### TicketReleasedEvent

**Topic**: `ticket-events`

**Payload**:
```json
{
  "eventType": "ticket.released",
  "eventId": "evt-uuid-128",
  "timestamp": "2026-03-21T10:05:01",
  "ticketId": "ticket-uuid-789",
  "userId": "user-uuid-789",
  "reason": "Payment failed",
  "correlationId": "corr-uuid-def"
}
```

---

## Error Responses

All APIs follow a consistent error response format:

**Error Response Structure**:
```json
{
  "timestamp": "2026-03-21T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Payment provider is required",
  "path": "/api/tickets/reserve"
}
```

**Common Status Codes**:
- `400` - Bad Request (validation errors, invalid input)
- `401` - Unauthorized (authentication required or invalid)
- `403` - Forbidden (insufficient permissions)
- `404` - Not Found (resource doesn't exist)
- `409` - Conflict (duplicate resource, seats already reserved)
- `500` - Internal Server Error (unexpected server error)

---

## Authentication & Authorization

**Current Status**: No authentication implemented (learning/demo environment)

**Production Recommendations**:
- Use JWT tokens for authentication
- OAuth 2.0 for third-party integrations
- Role-based access control (RBAC)
- API rate limiting
- HTTPS only

---

## Rate Limiting (Planned)

**Recommended Limits**:
- Public APIs: 100 requests/minute per IP
- Webhook endpoints: 1000 requests/minute (Razorpay may send many)
- Admin APIs: 500 requests/minute per user

---

## CORS Configuration

**Current**: All origins allowed (`*`) for development

**Production**: Restrict to specific origins
```java
@CrossOrigin(origins = {"https://yourdomain.com"})
```

---

## API Versioning

**Current**: No versioning (v1 implicit)

**Future**: Use URL-based versioning
- `/api/v1/tickets/reserve`
- `/api/v2/tickets/reserve`

---

## Postman Collection

Import this collection for testing:

**Collection URL**: [Link to Postman collection JSON]

**Pre-configured Requests**:
1. Reserve Ticket (Razorpay)
2. Reserve Ticket (Stripe)
3. Get Payment Status
4. Get Ticket Details
5. Get User Bookings
6. Get Audit Events

**Environment Variables**:
```
BASE_URL_TICKET=http://localhost:8081/api
BASE_URL_PAYMENT=http://localhost:8082/api
BASE_URL_USER=http://localhost:8084/api
BASE_URL_AUDIT=http://localhost:8085/api
```

---

## Testing with cURL

### Reserve Ticket with Razorpay

```bash
curl -X POST http://localhost:8081/api/tickets/reserve \
  -H "Content-Type: application/json" \
  -d '{
    "movieName": "Inception",
    "showTime": "2026-03-21T19:00:00",
    "seatNumbers": ["A1", "A2"],
    "userId": "user-123",
    "totalAmount": 150.00,
    "paymentProvider": "RAZORPAY"
  }'
```

### Poll for Payment Status

```bash
# Replace {ticketId} with actual ticket ID
curl http://localhost:8082/api/payments/ticket/{ticketId}/status
```

### Test Webhook (Local Testing)

```bash
# This will fail signature verification (for testing)
curl -X POST http://localhost:8082/api/payments/webhook/razorpay \
  -H "X-Razorpay-Signature: test_signature" \
  -H "Content-Type: application/json" \
  -d '{
    "entity": "event",
    "event": "payment.captured"
  }'
```

---

## Webhook Testing with ngrok

For testing Razorpay webhooks locally:

```bash
# Start ngrok
ngrok http 8082

# Update Razorpay webhook URL to:
https://xxxxx.ngrok.io/api/payments/webhook/razorpay

# Test by making a payment on Razorpay test page
```

---

## Health Check Endpoints

All services expose health endpoints via Spring Actuator:

**Endpoints**:
- `GET /actuator/health` - Service health status
- `GET /actuator/info` - Service information
- `GET /actuator/metrics` - Service metrics

**Example**:
```bash
curl http://localhost:8081/actuator/health
```

**Response**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "kafka": {
      "status": "UP"
    }
  }
}
```

---

## Support & Issues

For API issues or questions:
1. Check Swagger UI for interactive testing
2. Review logs: `docker-compose logs -f [service-name]`
3. GitHub Issues: https://github.com/frex-arup/sb-kafka-ticket/issues

---

## Changelog

### v1.1.0 (Current) - Payment Gateway Integration
- ✨ Added `GET /api/payments/ticket/{ticketId}/status`
- ✨ Added `POST /api/payments/webhook/razorpay`
- ✨ Added `GET /api/payments/stripe-failure-page/{paymentId}`
- 📝 Added `paymentProvider` field to ticket reservation
- 📝 New event: `PaymentInitiatedEvent`
- 🔒 Added webhook signature verification

### v1.0.0 - Initial Release
- Initial API implementation
- Basic CRUD operations
- Event-driven architecture
- Simulated payment processing
