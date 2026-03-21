# UI User Guide - Ticket Booking System

## Overview

The ticket booking system provides an intuitive Angular-based frontend for booking movie tickets with real payment processing. This guide explains how to use each feature of the UI.

---

## Dashboard (Home Page)

**URL**: http://localhost:4200/

### Features

1. **Welcome Section**
   - Overview of the system
   - Key features highlighted
   - Quick navigation buttons

2. **System Status**
   - Shows if services are running
   - Event flow diagram
   - Quick stats (optional)

3. **Quick Actions**
   - "Book Tickets" - Navigate to booking page
   - "View My Bookings" - See booking history
   - "Monitor Events" - Real-time event viewer
   - "Audit Logs" - Complete event history

---

## Movie Listing

**URL**: http://localhost:4200/movies

### Features

- Browse available movies
- View movie details (title, description, price)
- See available showtimes
- Click "Book Now" to start reservation

### Movie Card Layout

```
┌──────────────────────────┐
│   Movie Poster/Image     │
│                          │
├──────────────────────────┤
│ Movie Title              │
│ Description...           │
│ Price: ₹ 150.00          │
│ Showtime: 7:00 PM        │
│                          │
│    [Book Now]            │
└──────────────────────────┘
```

---

## Ticket Reservation (Booking Page)

**URL**: http://localhost:4200/booking

This is where you book tickets and select your payment method.

### Step 1: Fill Booking Form

**Form Fields**:

1. **Movie Name** (Text Input)
   - Enter movie name (e.g., "Inception")
   - Required field

2. **User ID** (Text Input)
   - Enter your user ID or email
   - Create user first via User Management (optional)
   - Required field

3. **Show Time** (DateTime Picker)
   - Select date and time for the show
   - Must be future date/time
   - Format: YYYY-MM-DD HH:MM
   - Required field

4. **Seat Numbers** (Text Input)
   - Enter comma-separated seat numbers
   - Example: "A1, A2, A3"
   - Format: Any alphanumeric (A1, B5, etc.)
   - Required field

5. **Total Amount** (Number Input)
   - Enter ticket price
   - Example: 150 (for ₹150.00)
   - Default: 150
   - Required field

6. **Payment Provider** (Dropdown) ⭐ NEW
   - **Razorpay (Real Payment)** - Process real payment with test cards
   - **Stripe (Test Failure)** - Simulate payment failure
   - Required field
   - **Default**: Razorpay

### Step 2: Submit Reservation

Click **"Reserve Tickets"** button

### Step 3: Wait for Payment Link

**For Razorpay Selection**:
- Loading indicator appears
- Message: "Preparing payment..."
- System polls for payment URL (~2-3 seconds)
- Once ready, you're automatically redirected

**For Stripe Selection**:
- Immediate failure message
- "Stripe payment failed (simulated)"
- No redirect (stays on page)
- Ticket is automatically released

---

## Razorpay Payment Flow

### Step 4: Complete Payment on Razorpay

After redirect, you'll see Razorpay's checkout page:

**Test Cards** (Use these in test mode):

| Card Number         | CVV | Expiry Date | Result  |
|---------------------|-----|-------------|---------|
| 4111 1111 1111 1111 | 123 | 12/25       | Success |
| 5555 5555 5555 4444 | 123 | 12/25       | Success |
| 4012 0010 3714 1112 | 123 | 12/25       | Failure |

**Payment Details to Enter**:
- Card Number: One of the above
- CVV: Any 3 digits
- Expiry Date: Any future date (MM/YY)
- Cardholder Name: Any name

### Step 5: Payment Confirmation

**On Success**:
- Razorpay shows "Payment Successful" message
- May redirect back to app (optional)
- Webhook is sent to backend (automatic)
- Ticket status updated to BOOKED

**On Failure**:
- Razorpay shows "Payment Failed" message
- Webhook sent to backend
- Ticket status updated to RELEASED
- User can try again

---

## My Bookings

**URL**: http://localhost:4200/my-bookings

View all your ticket bookings and their current status.

### Booking Card Layout

```
┌────────────────────────────────────┐
│ Movie: Inception                   │
│ Show Time: 2026-03-21 19:00        │
│ Seats: A1, A2                      │
│ Amount: ₹ 150.00                   │
│ Status: BOOKED ✅                   │
│ Payment Provider: RAZORPAY         │
│ Confirmation Code: ABC123          │
│ Booked At: 2026-03-21 10:30:00    │
└────────────────────────────────────┘
```

### Booking Status Indicators

| Status    | Icon | Color  | Meaning                           |
|-----------|------|--------|-----------------------------------|
| AVAILABLE | ⚪   | Grey   | Ticket available (not used here)  |
| RESERVED  | 🟡   | Yellow | Reserved, payment pending         |
| BOOKED    | ✅   | Green  | Payment completed, confirmed      |
| RELEASED  | ❌   | Red    | Payment failed, ticket released   |

### Features

- **Filter by Status**: Show only BOOKED or RELEASED tickets
- **Search**: Search by movie name or confirmation code
- **Refresh**: Reload booking list
- **View Details**: Click card to see full booking details

---

## Event Monitor

**URL**: http://localhost:4200/events

Real-time visualization of Kafka events flowing through the system.

### Purpose

- **Learning Tool**: See how events flow between services
- **Debugging**: Trace issues through the event chain
- **Understanding EDA**: Visualize event-driven architecture in action

### Event Display

Each event card shows:

```
┌────────────────────────────────────────┐
│ 💳 PAYMENT.INITIATED                   │
│ ──────────────────────────────────────│
│ Event ID: evt-123                      │
│ Timestamp: 2026-03-21 10:00:00        │
│ Topic: payment-events                  │
│ Partition: 1 | Offset: 42             │
│ Correlation ID: corr-456              │
│                                        │
│ [View Details]                         │
└────────────────────────────────────────┘
```

### Event Types and Colors

| Event Type              | Icon | Border Color | Description                    |
|-------------------------|------|--------------|--------------------------------|
| ticket.reserved         | 🎫   | Blue         | Ticket reservation created     |
| payment.initiated       | 💳   | Orange       | Payment link generated         |
| payment.completed       | ✅   | Green        | Payment successful             |
| payment.failed          | ❌   | Red          | Payment failed                 |
| ticket.booked           | 🎉   | Green        | Booking confirmed              |
| ticket.released         | 🔄   | Yellow       | Ticket released (failure)      |
| user.registered         | 👤   | Purple       | New user registered            |

### Features

1. **Auto-Refresh**
   - Toggle auto-refresh on/off
   - Refresh interval: 5 seconds

2. **Manual Refresh**
   - Click "Refresh" button
   - Fetches latest events

3. **Filter by Event Type**
   - Dropdown to filter specific event types
   - "All Events" to see everything

4. **View Event Details**
   - Click "View Details" to expand
   - Shows complete JSON payload
   - Displays all event fields

### Event Flow Visualization

For a successful booking:
```
ticket.reserved
    ↓
payment.initiated (contains payment URL)
    ↓
payment.completed (after user pays on Razorpay)
    ↓
ticket.booked
```

For a failed booking (Stripe):
```
ticket.reserved
    ↓
payment.failed (immediate)
    ↓
ticket.released
```

---

## Audit Logs

**URL**: http://localhost:4200/audit

Complete audit trail of all events stored in MongoDB (event sourcing).

### Purpose

- **Compliance**: Complete audit trail for regulations
- **Debugging**: Trace issues with full event history
- **Event Sourcing**: Demonstrates event sourcing pattern

### Search & Filter

1. **Search by Correlation ID**
   - Enter correlation ID (links related events)
   - Shows complete transaction flow
   - Example: `corr-abc-123`

2. **Filter by Event Type**
   - Select specific event type
   - View only relevant events

3. **Date Range Filter**
   - From Date: Start of range
   - To Date: End of range
   - Filter events within date range

4. **Search by Keyword**
   - Search in event payload
   - Example: "Inception" to find movie bookings

### Audit Log Entry

```
┌──────────────────────────────────────────────┐
│ Event Type: payment.completed                │
│ Event ID: evt-789                            │
│ Timestamp: 2026-03-21 10:05:00              │
│ Topic: payment-events                        │
│ Partition: 1 | Offset: 45                   │
│ Correlation ID: corr-456                    │
│                                              │
│ Event Data:                                  │
│ {                                            │
│   "paymentId": "pay-123",                   │
│   "ticketId": "ticket-456",                 │
│   "amount": 150.00,                         │
│   "transactionId": "txn-xyz"                │
│ }                                            │
└──────────────────────────────────────────────┘
```

### Features

1. **Export Audit Logs**
   - Download filtered logs as JSON
   - Export for reporting/analysis

2. **Correlation ID Tracing**
   - Click correlation ID to see all related events
   - Visualize complete transaction flow

3. **Event Reconstruction**
   - See how state evolved over time
   - Understand event sourcing pattern

---

## User Management (Optional)

**URL**: http://localhost:4200/users

Create and manage user profiles.

### Create User

**Form Fields**:
- Name (Text)
- Email (Email)
- Phone (Text, optional)

**Submit**: Click "Create User"

**Result**: User ID generated (UUID)

### View Users

- List all registered users
- Shows: Name, Email, User ID
- Click user to see booking history

---

## Common UI Patterns

### Status Messages

**Success**:
```
┌──────────────────────────────┐
│ ✅ Success!                   │
│ Ticket reserved successfully │
└──────────────────────────────┘
```

**Error**:
```
┌──────────────────────────────┐
│ ❌ Error!                     │
│ Payment failed. Try again.   │
└──────────────────────────────┘
```

**Info**:
```
┌──────────────────────────────┐
│ ℹ️ Processing...              │
│ Generating payment link...   │
└──────────────────────────────┘
```

### Loading Indicators

- Spinner: Circular loading animation
- Progress Bar: For longer operations
- Skeleton Screens: While loading data

---

## Payment Provider Selection Guide

### When to Use Razorpay

**Use Cases**:
- Testing real payment flow
- Understanding webhook integration
- Learning production payment patterns
- Demonstrating successful bookings

**What Happens**:
1. Payment link generated (2-3 seconds)
2. Redirected to Razorpay checkout
3. Enter test card details
4. Complete payment
5. Webhook received (automatic)
6. Booking confirmed

**Duration**: ~30-60 seconds total

### When to Use Stripe (Failure)

**Use Cases**:
- Testing error handling
- Demonstrating compensating transactions
- Understanding SAGA rollback
- Testing notification of failures

**What Happens**:
1. Immediate failure (< 1 second)
2. Error message displayed
3. Ticket automatically released
4. No redirect, stays on page

**Duration**: ~1 second total

---

## Keyboard Shortcuts (Planned)

| Shortcut  | Action              |
|-----------|---------------------|
| Ctrl + B  | Go to Booking page  |
| Ctrl + M  | Go to My Bookings   |
| Ctrl + E  | Go to Event Monitor |
| Ctrl + R  | Refresh current page|

---

## Troubleshooting

### Payment Link Not Appearing

**Symptoms**: Stuck on "Preparing payment..." for > 10 seconds

**Possible Causes**:
- Razorpay API key invalid
- Network connectivity issues
- Payment service down

**Solutions**:
1. Check browser console for errors
2. Verify payment-service logs: `docker-compose logs payment-service`
3. Check Razorpay API key configuration
4. Try again with Stripe to test if basic flow works

### Redirect Not Working

**Symptoms**: Payment link appears but redirect doesn't happen

**Possible Causes**:
- Browser popup blocker
- JavaScript error

**Solutions**:
1. Check browser console for errors
2. Manually click the payment URL link (should appear as fallback)
3. Disable popup blocker for localhost

### Booking Not Confirmed After Payment

**Symptoms**: Paid on Razorpay but ticket still RESERVED

**Possible Causes**:
- Webhook not received
- Webhook signature verification failed
- Payment service crashed

**Solutions**:
1. Check payment-service logs for webhook receipt
2. Look for signature verification errors
3. Check if payment record exists: GET `/api/payments/ticket/{ticketId}/status`
4. Razorpay dashboard → Webhooks → Check delivery logs

### Events Not Showing in Monitor

**Symptoms**: Event Monitor is empty or not updating

**Possible Causes**:
- Audit service not running
- MongoDB connection issue
- No events published yet

**Solutions**:
1. Book a ticket to generate events
2. Click "Refresh" manually
3. Check audit-service logs
4. Verify MongoDB is running: `docker-compose ps mongodb`

---

## Mobile Responsiveness

The UI is responsive and works on mobile devices:

**Breakpoints**:
- Mobile: < 768px (single column)
- Tablet: 768px - 1024px (adjusted layouts)
- Desktop: > 1024px (full layout)

**Mobile Optimizations**:
- Touch-friendly buttons (larger tap targets)
- Simplified navigation (hamburger menu)
- Reduced information density
- Stack layouts vertically

---

## Accessibility Features

- **Keyboard Navigation**: Tab through all interactive elements
- **Screen Reader Support**: ARIA labels on all controls
- **Color Contrast**: WCAG AA compliant
- **Focus Indicators**: Clear focus states for keyboard users

---

## Tips for Learning

### Trace a Complete Flow

1. Open Event Monitor in one tab
2. Open Booking page in another tab
3. Book a ticket with Razorpay
4. Watch events appear in real-time
5. Switch to My Bookings to see result
6. Check Audit Logs for complete history

### Understand Failure Handling

1. Book with Stripe (failure simulation)
2. Watch compensating transaction
3. See ticket.released event
4. Notice ticket doesn't appear in My Bookings
5. This demonstrates SAGA rollback pattern

### Explore Event Sourcing

1. Book several tickets (mix of success/failure)
2. Go to Audit Logs
3. Pick a Correlation ID from a successful booking
4. Search by that Correlation ID
5. See complete event chain: reserved → initiated → completed → booked

---

## Advanced Features (Coming Soon)

- WebSocket real-time updates (no polling needed)
- Payment status notifications
- Seat map visualization
- QR code generation for bookings
- Email/SMS notifications
- Booking cancellation with refund flow

---

## Support

For UI issues:
1. Check browser console (F12)
2. Verify backend services are running
3. Check network tab for API errors
4. Review logs in terminal/Docker

For questions or feedback:
- GitHub Issues: https://github.com/frex-arup/sb-kafka-ticket/issues
- Documentation: See /docs folder
