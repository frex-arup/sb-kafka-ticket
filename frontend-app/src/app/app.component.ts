import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterModule, MatToolbarModule, MatButtonModule, MatIconModule],
  template: `
    <mat-toolbar color="primary">
      <span>🎬 Ticket Booking - Learn Kafka & EDA</span>
      <span style="flex: 1 1 auto;"></span>
      <button mat-button routerLink="/">Dashboard</button>
      <button mat-button routerLink="/movies">Movies</button>
      <button mat-button routerLink="/reserve">Book Tickets</button>
      <button mat-button routerLink="/bookings">My Bookings</button>
      <button mat-button routerLink="/events">Event Monitor</button>
      <button mat-button routerLink="/audit">Audit Logs</button>
    </mat-toolbar>
    <router-outlet></router-outlet>
  `,
  styles: [`
    mat-toolbar { position: sticky; top: 0; z-index: 1000; }
  `]
})
export class AppComponent {
  title = 'Ticket Booking System';
}
