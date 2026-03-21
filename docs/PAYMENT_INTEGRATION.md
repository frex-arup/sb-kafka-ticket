# Payment Gateway Integration Guide

## Overview

The ticket booking system integrates with **real payment gateways** to process actual transactions. This document explains how the payment integration works, the supported providers, and the complete payment flow.

## Supported Payment Providers

### 1. Razorpay (Real Payment Processing)
- **Status**: ✅ Live Integration
- **Test Mode**: Yes (using test API keys)
- **Payment Method**: Payment Links API
- **Webhook Support**: Yes (signature verification enabled)
- **Use Case**: Real payment processing with test cards

### 2. Stripe (Simulated Failure)
- **Status**: ⚠️ Simulation Mode
- **Behavior**: Always returns payment failure
- **Purpose**: Testing compensating transactions and error handling
- **Webhook Support**: No (immediate failure, no callback needed)

---

## Payment Flow Architecture

### High-Level Flow

```
┌─────────────┐
│   Frontend  │ User selects payment provider (Razorpay/Stripe)
└──────┬──────┘
       │ POST /api/tickets/reserve (with paymentProvider)
       ↓
┌─────────────────┐
│ Ticket Service  │ Creates reservation → Publishes TicketReservedEvent
└──────┬──────────┘
       │ Kafka: ticket-events topic
       ↓
┌──────────────────┐
│ Payment Service  │ Consumes event → Creates payment link
└──────┬───────────┘
       │
       ├─→ IF STRIPE: Immediate failure → PaymentFailedEvent
       │
       └─→ IF RAZORPAY: Generate payment link → PaymentInitiatedEvent
                          │
                          ↓
                   ┌──────────────┐
                   │   Frontend   │ Polls /api/payments/ticket/{id}/status
                   └──────┬───────┘
                          │ Gets paymentUrl
                          ↓
                   ┌──────────────┐
                   │ Razorpay UI  │ User completes payment
                   └──────┬───────┘
                          │ Webhook callback
                          ↓
                   ┌──────────────────┐
                   │ Payment Service  │ Webhook handler verifies signature
                   └──────┬───────────┘
                          │
                          ├─→ Success: PaymentCompletedEvent
                          └─→ Failure: PaymentFailedEvent
                                 │
                                 ↓
                          ┌──────────────────┐
                          │  Ticket Service  │ Updates ticket status
                          └──────────────────┘
                                 │
                                 ├─→ BOOKED (on success)
                                 └─→ RELEASED (on failure)
```

---

## Detailed Component Breakdown

### 1. Payment Gateway Abstraction

**Interface**: `PaymentGateway`

```java
public interface PaymentGateway {
    PaymentLinkResponse createPaymentLink(PaymentLinkRequest request);
    boolean verifyWebhookSignature(String payload, String signature);
    PaymentVerificationResult verifyPayment(String gatewayOrderId);
}
```

**Purpose**: Provides a unified interface for all payment providers, enabling easy addition of new gateways without changing core logic.

**Implementations**:
- `RazorpayPaymentGateway` - Real integration with Razorpay API
- `StripePaymentGateway` - Simulated failure for testing

---

### 2. Razorpay Integration Details

#### Payment Link Creation

**Process**:
1. Payment Service receives `TicketReservedEvent` with `paymentProvider=RAZORPAY`
2. Creates Razorpay Order via Orders API
3. Generates Payment Link with order details
4. Saves payment URL, gateway order ID, and expiry time to database
5. Publishes `PaymentInitiatedEvent` with payment URL

**API Endpoints Used**:
- `POST /v1/orders` - Create Razorpay Order
- `POST /v1/payment_links` - Generate Payment Link

**Payment Link Structure**:
```json
{
  "amount": 15000,  // Amount in paise (₹150.00)
  "currency": "INR",
  "description": "Ticket booking for Inception",
  "reference_id": "payment-uuid",
  "customer": {
    "name": "John Doe",
    "email": "john@example.com"
  },
  "callback_url": "http://localhost:8082/api/payments/webhook/razorpay",
  "callback_method": "get"
}
```

#### Webhook Handling

**Endpoint**: `POST /api/payments/webhook/razorpay`

**Security**: HMAC-SHA256 signature verification
```java
// Razorpay sends X-Razorpay-Signature header
boolean isValid = Utils.verifyWebhookSignature(payload, signature, webhookSecret);
```

**Webhook Payload Structure**:
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
        "method": "card"
      }
    }
  }
}
```

**Processing Steps**:
1. Verify webhook signature (reject if invalid)
2. Extract order ID from payload
3. Find payment record by `paymentGatewayOrderId`
4. Verify payment status with Razorpay API (double-check)
5. Update payment status in database
6. Publish `PaymentCompletedEvent` or `PaymentFailedEvent`

#### Test Cards

Use these test cards on Razorpay's payment page:

| Card Number         | CVV | Expiry  | Behavior        |
|---------------------|-----|---------|-----------------|
| 4111 1111 1111 1111 | Any | Future  | Success         |
| 4012 0010 3714 1112 | Any | Future  | Failure         |
| 5555 5555 5555 4444 | Any | Future  | Success (MasterCard) |

---

### 3. Stripe Integration (Failure Simulation)

#### Behavior

**Purpose**: Demonstrate compensating transactions and error handling in the SAGA pattern.

**Process**:
1. Payment Service receives `TicketReservedEvent` with `paymentProvider=STRIPE`
2. Immediately sets payment status to FAILED
3. Sets failure reason: "Stripe payment declined (simulated failure for testing)"
4. Publishes `PaymentFailedEvent`
5. Ticket Service consumes event and releases the ticket

**No Real API Calls**: Stripe SDK is included but never invoked. This is intentional for testing purposes.

**Mock Payment URL**: Points to local failure page
```
http://localhost:8082/api/payments/stripe-failure-page/{paymentId}
```

---

## Events Published

### PaymentInitiatedEvent (NEW)

**Topic**: `payment-events`

**Published When**: Payment link is successfully generated

**Payload**:
```json
{
  "eventType": "payment.initiated",
  "eventId": "uuid",
  "timestamp": "2026-03-21T10:00:00",
  "paymentId": "payment-uuid",
  "ticketId": "ticket-uuid",
  "userId": "user-uuid",
  "paymentUrl": "https://rzp.io/l/xxxxx",
  "paymentProvider": "RAZORPAY",
  "amount": 150.00,
  "expiresAt": "2026-03-21T10:15:00",
  "correlationId": "correlation-uuid"
}
```

**Consumers**: Frontend polls payment status endpoint to get this URL

### PaymentCompletedEvent (Updated)

**Published When**: Payment is successfully verified via webhook

**Key Change**: Now triggered by webhook callback instead of immediate simulation

### PaymentFailedEvent (Updated)

**Published When**:
- Webhook indicates payment failure
- Stripe provider selected (immediate)
- Payment link creation fails

---

## Database Schema Changes

### Payment Table

**New Columns**:
```sql
ALTER TABLE payments
  ADD COLUMN payment_provider VARCHAR(50),
  ADD COLUMN payment_gateway_order_id VARCHAR(255),
  ADD COLUMN payment_url TEXT,
  ADD COLUMN payment_expires_at TIMESTAMP;

CREATE INDEX idx_payment_gateway_order_id ON payments(payment_gateway_order_id);
```

**Column Descriptions**:
- `payment_provider`: "RAZORPAY", "STRIPE", or "SIMULATED"
- `payment_gateway_order_id`: Gateway's order/payment ID (for webhook correlation)
- `payment_url`: Payment link URL (for frontend redirect)
- `payment_expires_at`: Payment link expiry time (typically 15 minutes)

**Example Record**:
```
id: pay-123
ticketId: ticket-456
userId: user-789
amount: 150.00
status: PENDING
paymentMethod: ONLINE
paymentProvider: RAZORPAY
paymentGatewayOrderId: order_xxxxx
paymentUrl: https://rzp.io/l/xxxxx
paymentExpiresAt: 2026-03-21T10:15:00
```

---

## API Endpoints

### Payment Service Endpoints

#### 1. Razorpay Webhook
```
POST /api/payments/webhook/razorpay
Header: X-Razorpay-Signature (required)
Body: Raw JSON payload from Razorpay
Response: 200 OK / 401 Unauthorized
```

#### 2. Get Payment Status
```
GET /api/payments/ticket/{ticketId}/status
Response:
{
  "paymentId": "uuid",
  "ticketId": "uuid",
  "status": "PENDING | COMPLETED | FAILED",
  "paymentProvider": "RAZORPAY",
  "paymentUrl": "https://rzp.io/l/xxxxx",
  "amount": 150.00,
  "transactionId": "pay_xxxxx",
  "failureReason": null,
  "createdAt": "2026-03-21T10:00:00",
  "updatedAt": "2026-03-21T10:05:00"
}
```

#### 3. Stripe Failure Page (Mock)
```
GET /api/payments/stripe-failure-page/{paymentId}
Response: HTML page with failure message
```

---

## Frontend Integration

### 1. Payment Provider Selection

**Component**: `TicketReservationComponent`

**UI Element**: Dropdown/Radio buttons
```html
<mat-form-field appearance="outline">
  <mat-label>Payment Provider</mat-label>
  <mat-select [(ngModel)]="paymentProvider" name="provider" required>
    <mat-option value="RAZORPAY">Razorpay (Real Payment)</mat-option>
    <mat-option value="STRIPE">Stripe (Test Failure)</mat-option>
  </mat-select>
</mat-form-field>
```

### 2. Payment Initiation

**Request**:
```typescript
const request: TicketRequest = {
  movieName: "Inception",
  showTime: "2026-03-21T19:00:00",
  seatNumbers: ["A1", "A2"],
  userId: "user-123",
  totalAmount: 150.00,
  paymentProvider: "RAZORPAY"  // NEW FIELD
};

this.ticketService.reserveTicket(request).subscribe();
```

### 3. Payment URL Polling

**Why Polling**: Payment link generation may take 1-2 seconds due to API calls.

**Implementation**:
```typescript
pollForPaymentUrl(ticketId: string) {
  const pollInterval = setInterval(() => {
    this.paymentService.getPaymentStatus(ticketId).subscribe({
      next: (status) => {
        if (status.paymentUrl) {
          clearInterval(pollInterval);

          if (status.paymentProvider === 'STRIPE') {
            // Show immediate failure
            this.showError('Stripe payment failed');
          } else {
            // Redirect to Razorpay
            window.location.href = status.paymentUrl;
          }
        }
      }
    });
  }, 2000); // Poll every 2 seconds
}
```

### 4. Payment Completion

**User Flow**:
1. User completes payment on Razorpay page
2. Razorpay redirects to callback URL (optional)
3. Webhook updates payment status in background
4. Frontend can poll for status or show success message
5. User navigates to "My Bookings" to see confirmed ticket

---

## Configuration

### Environment Variables

**Required for Production**:
```bash
# Razorpay Configuration
export RAZORPAY_KEY_ID=rzp_live_xxxxx
export RAZORPAY_KEY_SECRET=your_secret_key
export RAZORPAY_WEBHOOK_SECRET=your_webhook_secret

# Base URL (for webhook callbacks)
export SERVER_BASE_URL=https://api.yourdomain.com
```

### application.yml

```yaml
payment:
  razorpay:
    key-id: ${RAZORPAY_KEY_ID:rzp_test_SCRb6wY1aDgKKz}
    key-secret: ${RAZORPAY_KEY_SECRET:LslF4Agepo446qQxhRyzEr50}
    webhook-secret: ${RAZORPAY_WEBHOOK_SECRET:changeme}
    callback-url: ${SERVER_BASE_URL:http://localhost:8082}/api/payments/webhook/razorpay

  stripe:
    simulate-failure: true

server:
  base-url: ${SERVER_BASE_URL:http://localhost:8082}
```

---

## Security Considerations

### 1. Webhook Signature Verification

**Critical**: Always verify webhook signatures before processing.

```java
if (!gateway.verifyWebhookSignature(payload, signature)) {
    log.error("Invalid webhook signature - possible fraud attempt");
    throw new SecurityException("Invalid signature");
}
```

**Why**: Prevents attackers from sending fake payment success webhooks.

### 2. API Key Protection

**Best Practices**:
- ✅ Store keys in environment variables
- ✅ Never commit keys to version control
- ✅ Use different keys for dev/staging/production
- ✅ Rotate keys periodically
- ❌ Never hardcode keys in application.yml

### 3. HTTPS Requirement

**Production**: Payment gateway webhooks REQUIRE HTTPS.

**Local Testing**: Use ngrok for webhook testing
```bash
ngrok http 8082
# Update webhook URL to: https://xxxxx.ngrok.io/api/payments/webhook/razorpay
```

### 4. Idempotency

**Prevention**: Duplicate webhook handling

```java
if (payment.getStatus() != PaymentStatus.PENDING) {
    log.info("Payment already processed, ignoring duplicate webhook");
    return;
}
```

---

## Testing Guide

### 1. Razorpay Happy Path

**Steps**:
1. Select "Razorpay" in frontend
2. Fill ticket reservation form and submit
3. Wait for redirect to Razorpay payment page (~2 seconds)
4. Use test card: `4111 1111 1111 1111`
5. Complete payment
6. Webhook is received and verified
7. Check booking status in "My Bookings" (should be BOOKED)

**Expected Logs**:
```
payment-service | Creating Razorpay payment link for order: pay-123
payment-service | Razorpay order created: order_xxxxx
payment-service | Publishing PaymentInitiatedEvent
payment-service | Received Razorpay webhook
payment-service | Razorpay webhook signature verified successfully
payment-service | Payment successful: paymentId=pay-123
ticket-service  | Received PaymentCompletedEvent
ticket-service  | Ticket status updated to BOOKED
```

### 2. Stripe Failure Path

**Steps**:
1. Select "Stripe" in frontend
2. Submit reservation
3. Immediate failure message appears
4. Check ticket status (should be RELEASED)

**Expected Logs**:
```
payment-service | Stripe payment - simulating immediate failure
payment-service | Payment failed: reason=Stripe payment declined
ticket-service  | Received PaymentFailedEvent
ticket-service  | Releasing ticket due to payment failure
ticket-service  | Publishing TicketReleasedEvent
```

### 3. Webhook Security Test

**Test Invalid Signature**:
```bash
curl -X POST http://localhost:8082/api/payments/webhook/razorpay \
  -H "X-Razorpay-Signature: invalid_signature" \
  -H "Content-Type: application/json" \
  -d '{"event":"payment.captured"}'
```

**Expected Response**: `401 Unauthorized`

---

## Troubleshooting

### Issue: Payment link not appearing after reservation

**Possible Causes**:
1. Razorpay API keys invalid
2. Network connectivity issues
3. Razorpay API rate limiting

**Debug**:
```bash
# Check payment-service logs
docker-compose logs -f payment-service

# Look for:
# - "Creating Razorpay payment link"
# - Error messages from Razorpay SDK
```

### Issue: Webhook not received

**Possible Causes**:
1. Webhook URL not accessible from internet
2. Webhook secret mismatch
3. Firewall blocking Razorpay IPs

**Solutions**:
- Use ngrok for local testing
- Check Razorpay dashboard webhook logs
- Verify webhook secret matches configuration

### Issue: Payment stuck in PENDING

**Possible Causes**:
1. Webhook signature verification failed
2. Payment record not found by gateway order ID
3. Exception in webhook handler

**Debug**:
```bash
# Check for webhook errors
docker-compose logs payment-service | grep "webhook"

# Manually check payment status
curl http://localhost:8082/api/payments/ticket/{ticketId}/status
```

---

## Adding New Payment Providers

### Steps to Add New Gateway (e.g., PayPal)

1. **Create Gateway Implementation**
   ```java
   @Component
   public class PayPalPaymentGateway implements PaymentGateway {
       // Implement interface methods
   }
   ```

2. **Update Factory**
   ```java
   case "PAYPAL" -> paypalGateway;
   ```

3. **Add Configuration**
   ```yaml
   payment:
     paypal:
       client-id: ${PAYPAL_CLIENT_ID}
       client-secret: ${PAYPAL_CLIENT_SECRET}
   ```

4. **Add Webhook Endpoint**
   ```java
   @PostMapping("/webhook/paypal")
   public ResponseEntity<String> handlePayPalWebhook(...)
   ```

5. **Update Frontend**
   ```html
   <mat-option value="PAYPAL">PayPal</mat-option>
   ```

---

## Monitoring & Observability

### Key Metrics to Track

1. **Payment Success Rate**
   ```
   successful_payments / total_payments * 100
   ```

2. **Webhook Processing Time**
   - Target: < 200ms
   - Alert if > 1 second

3. **Payment Link Generation Time**
   - Target: < 2 seconds
   - Includes Razorpay API calls

4. **Failed Signature Verifications**
   - Alert on any occurrence (security concern)

### Log Patterns to Monitor

```bash
# Payment failures
grep "Payment failed" payment-service.log

# Webhook signature failures
grep "Invalid webhook signature" payment-service.log

# Razorpay API errors
grep "RazorpayException" payment-service.log
```

---

## Production Checklist

Before deploying to production:

- [ ] Replace test API keys with live keys
- [ ] Configure HTTPS for webhook endpoints
- [ ] Set up webhook URL in Razorpay dashboard
- [ ] Test webhook signature verification with live keys
- [ ] Configure proper error alerting
- [ ] Set up payment reconciliation process
- [ ] Enable rate limiting on webhook endpoints
- [ ] Configure CORS properly (not `origins = "*"`)
- [ ] Set up backup webhook endpoint
- [ ] Document payment dispute handling process
- [ ] Set up payment audit logging
- [ ] Configure payment timeout handling (15 min link expiry)

---

## References

- [Razorpay Payment Links API Documentation](https://razorpay.com/docs/api/payment-links/)
- [Razorpay Webhook Documentation](https://razorpay.com/docs/webhooks/)
- [Razorpay Test Cards](https://razorpay.com/docs/payments/payments/test-card-details/)
- [Spring Kafka Documentation](https://docs.spring.io/spring-kafka/reference/html/)

---

## Support

For payment integration issues:
1. Check logs: `docker-compose logs payment-service`
2. Review Razorpay dashboard webhook logs
3. Test with different test cards
4. Verify API key validity in Razorpay dashboard
5. Check GitHub Issues: https://github.com/frex-arup/sb-kafka-ticket/issues
