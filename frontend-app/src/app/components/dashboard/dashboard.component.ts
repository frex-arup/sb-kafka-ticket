import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule],
  template: `
    <div class="container">
      <h1>🎬 Welcome to Kafka Event-Driven Ticket Booking</h1>
      <p>This application demonstrates Event-Driven Architecture (EDA) with Apache Kafka</p>

      <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(300px, 1fr)); gap: 20px; margin-top: 30px;">
        <mat-card>
          <mat-card-header>
            <mat-card-title>🎟️ Book Tickets</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>Reserve tickets for your favorite movies. Watch events flow through Kafka in real-time!</p>
          </mat-card-content>
          <mat-card-actions>
            <button mat-raised-button color="primary" routerLink="/reserve">Reserve Now</button>
          </mat-card-actions>
        </mat-card>

        <mat-card>
          <mat-card-header>
            <mat-card-title>📊 Event Monitor</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>See Kafka events in real-time. Watch the SAGA pattern in action!</p>
          </mat-card-content>
          <mat-card-actions>
            <button mat-raised-button color="accent" routerLink="/events">View Events</button>
          </mat-card-actions>
        </mat-card>

        <mat-card>
          <mat-card-header>
            <mat-card-title>📝 Audit Trail</mat-card-title>
          </mat-card-header>
          <mat-card-content>
            <p>Complete event sourcing and audit logs stored in MongoDB.</p>
          </mat-card-content>
          <mat-card-actions>
            <button mat-raised-button color="warn" routerLink="/audit">View Audit</button>
          </mat-card-actions>
        </mat-card>
      </div>

      <mat-card style="margin-top: 30px;">
        <mat-card-header>
          <mat-card-title>🎓 Learning Goals</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <ul>
            <li><strong>SAGA Pattern:</strong> See how distributed transactions work across microservices</li>
            <li><strong>Event Sourcing:</strong> Every state change is recorded as an event</li>
            <li><strong>Compensating Transactions:</strong> Watch how failures are handled (20% payment failure rate)</li>
            <li><strong>Fan-out Pattern:</strong> One event triggers multiple consumers</li>
            <li><strong>Kafka Fundamentals:</strong> Topics, partitions, producers, consumers, consumer groups</li>
          </ul>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .container { padding: 20px; max-width: 1200px; margin: 0 auto; }
    h1 { color: #3f51b5; }
  `]
})
export class DashboardComponent {
}
