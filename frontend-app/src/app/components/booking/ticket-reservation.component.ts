import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { switchMap, map, catchError } from 'rxjs/operators';
import { of } from 'rxjs';
import { TicketService } from '../../services/ticket.service';
import { PaymentService } from '../../services/payment.service';
import { UserService } from '../../services/user.service';

interface ReservationForm {
  movieName: string;
  userId: string;
  showTime: string;
  totalAmount: number;
  paymentProvider: 'RAZORPAY' | 'STRIPE' | 'SIMULATED';
}

interface ReservationResult {
  success: boolean;
  message: string;
  data?: any;
}

@Component({
  selector: 'app-ticket-reservation',
  standalone: true,
  imports: [CommonModule, FormsModule, MatCardModule, MatFormFieldModule, MatInputModule,
            MatButtonModule, MatSelectModule, MatProgressSpinnerModule],
  template: `
    <div class="container">
      <h1>🎟️ Reserve Tickets</h1>

      <mat-card>
        <mat-card-content>
          <form (ngSubmit)="reserveTicket()">
            <mat-form-field appearance="outline" style="width: 100%;">
              <mat-label>Movie Name</mat-label>
              <input matInput [(ngModel)]="reservation.movieName" name="movieName" required>
            </mat-form-field>

            <mat-form-field appearance="outline" style="width: 100%;">
              <mat-label>User ID (or create new user first)</mat-label>
              <input matInput [(ngModel)]="reservation.userId" name="userId" required>
            </mat-form-field>

            <mat-form-field appearance="outline" style="width: 100%;">
              <mat-label>Show Time</mat-label>
              <input matInput type="datetime-local" [(ngModel)]="reservation.showTime" name="showTime" required>
            </mat-form-field>

            <mat-form-field appearance="outline" style="width: 100%;">
              <mat-label>Seat Numbers (comma separated)</mat-label>
              <input matInput [(ngModel)]="seatInput" name="seats" placeholder="A1, A2, A3" required>
            </mat-form-field>

            <mat-form-field appearance="outline" style="width: 100%;">
              <mat-label>Total Amount (INR)</mat-label>
              <input matInput type="number" [(ngModel)]="reservation.totalAmount" name="amount" required>
            </mat-form-field>

            <mat-form-field appearance="outline" style="width: 100%;">
              <mat-label>Payment Provider</mat-label>
              <mat-select [(ngModel)]="reservation.paymentProvider" name="provider" required>
                <mat-option value="RAZORPAY">
                  <strong>💳 Razorpay</strong> (Real Payment - Test Mode)
                </mat-option>
                <mat-option value="STRIPE">
                  <strong>❌ Stripe</strong> (Simulated Failure)
                </mat-option>
                <mat-option value="SIMULATED">
                  <strong>🎲 Simulated</strong> (80% Success)
                </mat-option>
              </mat-select>
            </mat-form-field>

            <button mat-raised-button color="primary" type="submit" [disabled]="loading">
              <span *ngIf="!loading">Reserve Tickets</span>
              <mat-spinner *ngIf="loading" diameter="20"></mat-spinner>
            </button>
          </form>

          <div *ngIf="result" [class]="result.success ? 'success' : 'error'" style="margin-top: 20px; padding: 15px; border-radius: 4px;">
            <h3>{{ result.success ? '✅ Success!' : '❌ Error' }}</h3>
            <p>{{ result.message }}</p>
            <pre *ngIf="result.data">{{ result.data | json }}</pre>
          </div>

          <div style="margin-top: 20px; padding: 15px; background: #f5f5f5; border-radius: 4px;">
            <h4>💡 Payment Flow (BookMyShow-style):</h4>
            <p><strong>Razorpay:</strong> Real payment gateway - instant redirect to Razorpay test page</p>
            <p><strong>Stripe:</strong> Always fails - demonstrates error handling & SAGA compensation</p>
            <p><strong>Simulated:</strong> Instant success - for testing</p>
            <ol>
              <li>Reserve ticket → Create payment link → Redirect (all instant!)</li>
              <li>Complete payment on Razorpay test page</li>
              <li>Webhook confirms payment → Ticket booked!</li>
              <li>You'll be redirected back to "My Bookings"</li>
            </ol>
          </div>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .container { padding: 20px; max-width: 800px; margin: 0 auto; }
    .success { background: #e8f5e9; border: 1px solid #4caf50; }
    .error { background: #ffebee; border: 1px solid #f44336; }
  `]
})
export class TicketReservationComponent implements OnInit {
  reservation: ReservationForm = {
    movieName: '',
    userId: '',
    showTime: '',
    totalAmount: 150,
    paymentProvider: 'RAZORPAY'
  };
  seatInput = '';
  loading = false;
  result: ReservationResult | null = null;

  constructor(
    private ticketService: TicketService,
    private paymentService: PaymentService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      if (params['movie']) this.reservation.movieName = params['movie'];
      if (params['price']) this.reservation.totalAmount = params['price'];
    });
  }

  reserveTicket() {
    this.loading = true;
    this.result = null;

    const request = {
      ...this.reservation,
      seatNumbers: this.seatInput.split(',').map(s => s.trim()),
      showTime: new Date(this.reservation.showTime).toISOString()
    };

    // Flatten nested subscriptions using RxJS operators
    this.ticketService.reserveTicket(request).pipe(
      switchMap(ticket => {
        this.result = {
          success: true,
          message: `Ticket reserved! Preparing payment...`,
          data: ticket
        };

        const paymentRequest = {
          ticketId: ticket.id,
          userId: this.reservation.userId,
          amount: this.reservation.totalAmount,
          paymentProvider: this.reservation.paymentProvider
        };

        return this.paymentService.initiatePayment(paymentRequest).pipe(
          map(paymentResponse => ({ ticket, paymentResponse }))
        );
      }),
      catchError(err => {
        this.loading = false;
        this.result = {
          success: false,
          message: err.error?.message || 'Failed to reserve ticket or initiate payment.'
        };
        return of(null);
      })
    ).subscribe({
      next: (result) => {
        if (!result) return;

        this.loading = false;
        const { paymentResponse } = result;

        if (paymentResponse.status === 'PENDING' && paymentResponse.paymentUrl) {
          // Razorpay - redirect immediately
          this.result = {
            success: true,
            message: 'Redirecting to payment page...'
          };
          setTimeout(() => {
            window.location.href = paymentResponse.paymentUrl;
          }, 500);
        } else if (paymentResponse.status === 'FAILED') {
          // Stripe failure
          this.result = {
            success: false,
            message: paymentResponse.message || 'Payment failed'
          };
        } else if (paymentResponse.status === 'COMPLETED') {
          // Simulated success
          this.result = {
            success: true,
            message: 'Payment completed! Check "My Bookings".'
          };
        }
      }
    });
  }

  ngOnDestroy() {
    // Cleanup if needed
  }
}
