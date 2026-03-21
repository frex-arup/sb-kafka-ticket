import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { TicketService } from '../../services/ticket.service';
import { UserService } from '../../services/user.service';

@Component({
  selector: 'app-my-bookings',
  standalone: true,
  imports: [CommonModule, FormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatTableModule],
  template: `
    <div class="container">
      <h1>📚 My Bookings</h1>

      <mat-card>
        <mat-card-content>
          <mat-form-field appearance="outline">
            <mat-label>Enter User ID</mat-label>
            <input matInput [(ngModel)]="userId" (keyup.enter)="loadBookings()">
          </mat-form-field>
          <button mat-raised-button color="primary" (click)="loadBookings()" style="margin-left: 10px;">
            Load Bookings
          </button>
        </mat-card-content>
      </mat-card>

      <mat-card *ngIf="tickets.length > 0" style="margin-top: 20px;">
        <mat-card-header>
          <mat-card-title>Your Tickets</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <table mat-table [dataSource]="tickets" class="mat-elevation-z0" style="width: 100%;">
            <ng-container matColumnDef="id">
              <th mat-header-cell *matHeaderCellDef>Ticket ID</th>
              <td mat-cell *matCellDef="let ticket">{{ ticket.id }}</td>
            </ng-container>

            <ng-container matColumnDef="movieName">
              <th mat-header-cell *matHeaderCellDef>Movie</th>
              <td mat-cell *matCellDef="let ticket">{{ ticket.movieName }}</td>
            </ng-container>

            <ng-container matColumnDef="seats">
              <th mat-header-cell *matHeaderCellDef>Seats</th>
              <td mat-cell *matCellDef="let ticket">{{ ticket.seatNumbers?.join(', ') }}</td>
            </ng-container>

            <ng-container matColumnDef="status">
              <th mat-header-cell *matHeaderCellDef>Status</th>
              <td mat-cell *matCellDef="let ticket">
                <span [class]="getStatusClass(ticket.status)">{{ ticket.status }}</span>
              </td>
            </ng-container>

            <ng-container matColumnDef="confirmation">
              <th mat-header-cell *matHeaderCellDef>Confirmation</th>
              <td mat-cell *matCellDef="let ticket">{{ ticket.confirmationCode || '-' }}</td>
            </ng-container>

            <tr mat-header-row *matHeaderRowDef="displayedColumns"></tr>
            <tr mat-row *matRowDef="let row; columns: displayedColumns;"></tr>
          </table>
        </mat-card-content>
      </mat-card>

      <div *ngIf="tickets.length === 0 && userId" style="margin-top: 20px; text-align: center; color: #666;">
        No bookings found for this user.
      </div>
    </div>
  `,
  styles: [`
    .container { padding: 20px; }
    .BOOKED { color: #4caf50; font-weight: bold; }
    .RESERVED { color: #ff9800; font-weight: bold; }
    .RELEASED { color: #f44336; font-weight: bold; }
  `]
})
export class MyBookingsComponent implements OnInit {
  userId = '';
  tickets: any[] = [];
  displayedColumns = ['id', 'movieName', 'seats', 'status', 'confirmation'];

  constructor(private ticketService: TicketService) {}

  ngOnInit() {}

  loadBookings() {
    if (!this.userId) return;

    this.ticketService.getUserTickets(this.userId).subscribe({
      next: (tickets) => this.tickets = tickets,
      error: (err) => console.error('Failed to load bookings', err)
    });
  }

  getStatusClass(status: string): string {
    return status;
  }
}
