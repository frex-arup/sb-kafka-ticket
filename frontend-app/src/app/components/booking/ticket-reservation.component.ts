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
import { TicketService } from '../../services/ticket.service';
import { UserService } from '../../services/user.service';

interface ReservationForm {
  movieName: string;
  userId: string;
  showTime: string;
  totalAmount: number;
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
              <mat-label>Total Amount</mat-label>
              <input matInput type="number" [(ngModel)]="reservation.totalAmount" name="amount" required>
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
            <h4>💡 Learning Note:</h4>
            <p>When you click "Reserve Tickets":</p>
            <ol>
              <li>Ticket Service saves reservation to DB</li>
              <li>Publishes <code>ticket.reserved</code> event to Kafka</li>
              <li>Payment Service consumes event & processes payment (80% success rate)</li>
              <li>Publishes <code>payment.completed</code> or <code>payment.failed</code></li>
              <li>Ticket Service reacts accordingly (SAGA pattern)</li>
              <li>Check "Event Monitor" to see events flowing!</li>
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
  reservation: ReservationForm = { movieName: '', userId: '', showTime: '', totalAmount: 15 };
  seatInput = '';
  loading = false;
  result: ReservationResult | null = null;

  constructor(
    private ticketService: TicketService,
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

    this.ticketService.reserveTicket(request).subscribe({
      next: (ticket) => {
        this.loading = false;
        this.result = {
          success: true,
          message: `Ticket reserved! ID: ${ticket.id}. Payment processing... Check "My Bookings" in a moment.`,
          data: ticket
        };
      },
      error: (err) => {
        this.loading = false;
        this.result = {
          success: false,
          message: err.error?.message || 'Failed to reserve ticket. Please try again.'
        };
      }
    });
  }
}
