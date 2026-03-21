import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { PaymentStatus } from '../models/ticket.model';

export interface PaymentInitiateRequest {
  ticketId: string;
  userId: string;
  amount: number;
  paymentProvider: string;
}

export interface PaymentInitiateResponse {
  paymentId: string;
  paymentUrl: string;
  status: string;
  message: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentService {
  private apiUrl = `${environment.paymentServiceUrl}/payments`;

  constructor(private http: HttpClient) {}

  initiatePayment(request: PaymentInitiateRequest): Observable<PaymentInitiateResponse> {
    return this.http.post<PaymentInitiateResponse>(`${this.apiUrl}/initiate`, request);
  }

  getPaymentStatus(ticketId: string): Observable<PaymentStatus> {
    return this.http.get<PaymentStatus>(`${this.apiUrl}/ticket/${ticketId}/status`);
  }
}
