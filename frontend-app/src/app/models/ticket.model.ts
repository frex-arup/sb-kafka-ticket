export interface Ticket {
  id: string;
  movieName: string;
  showTime: string;
  seatNumbers: string[];
  userId: string;
  totalAmount: number;
  status: 'AVAILABLE' | 'RESERVED' | 'BOOKED' | 'RELEASED';
  reservedUntil?: string;
  confirmationCode?: string;
  createdAt: string;
  updatedAt: string;
}

export interface TicketRequest {
  movieName: string;
  showTime: string;
  seatNumbers: string[];
  userId: string;
  totalAmount: number;
  paymentProvider: 'RAZORPAY' | 'STRIPE' | 'SIMULATED';
}

export interface PaymentStatus {
  paymentId: string;
  ticketId: string;
  status: 'PENDING' | 'COMPLETED' | 'FAILED';
  paymentProvider: string;
  paymentUrl?: string;
  amount: number;
  transactionId?: string;
  failureReason?: string;
  createdAt: string;
  updatedAt: string;
}

export interface User {
  id: string;
  name: string;
  email: string;
  phone?: string;
  createdAt: string;
}

export interface AuditLog {
  id: string;
  eventId: string;
  eventType: string;
  correlationId: string;
  topic: string;
  partition: number;
  offset: number;
  eventData: any;
  timestamp: string;
  auditedAt: string;
}
