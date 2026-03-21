import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { AuditService } from '../../services/audit.service';

@Component({
  selector: 'app-event-monitor',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule],
  template: `
    <div class="container">
      <h1>📊 Real-Time Event Monitor</h1>
      <p>Watch Kafka events flow through the system</p>

      <button mat-raised-button color="primary" (click)="loadEvents()" style="margin-bottom: 20px;">
        🔄 Refresh Events
      </button>

      <div class="events-container">
        <mat-card *ngFor="let event of events" [class]="getEventClass(event.eventType)">
          <mat-card-header>
            <mat-card-title>{{ getEventIcon(event.eventType) }} {{ event.eventType }}</mat-card-title>
            <mat-card-subtitle>{{ event.timestamp | date:'medium' }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <p><strong>Event ID:</strong> {{ event.eventId }}</p>
            <p><strong>Correlation ID:</strong> {{ event.correlationId }}</p>
            <p><strong>Topic:</strong> {{ event.topic }} (partition: {{ event.partition }}, offset: {{ event.offset }})</p>
            <details>
              <summary style="cursor: pointer; color: #3f51b5;">View Event Data</summary>
              <pre style="background: #f5f5f5; padding: 10px; border-radius: 4px; overflow-x: auto;">{{ event.eventData | json }}</pre>
            </details>
          </mat-card-content>
        </mat-card>
      </div>

      <div *ngIf="events.length === 0" style="text-align: center; padding: 40px; color: #666;">
        No events yet. Start by reserving a ticket!
      </div>

      <mat-card style="margin-top: 30px; background: #e3f2fd;">
        <mat-card-header>
          <mat-card-title>🎓 Understanding the Event Flow</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <p><strong>SAGA Pattern in Action:</strong></p>
          <ol>
            <li><strong style="color: #2196f3;">ticket.reserved</strong> - User reserves tickets</li>
            <li><strong style="color: #ff9800;">payment.completed/failed</strong> - Payment processing result</li>
            <li><strong style="color: #4caf50;">ticket.booked</strong> - Booking confirmed (success path)</li>
            <li><strong style="color: #f44336;">ticket.released</strong> - Compensating transaction (failure path)</li>
          </ol>
          <p>Each event triggers multiple consumers independently. This is the power of event-driven architecture!</p>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .container { padding: 20px; }
    .events-container { display: grid; gap: 15px; }
    .ticket-event { border-left: 4px solid #2196f3; }
    .payment-event { border-left: 4px solid #ff9800; }
    .user-event { border-left: 4px solid #9c27b0; }
  `]
})
export class EventMonitorComponent implements OnInit {
  events: any[] = [];

  constructor(private auditService: AuditService) {}

  ngOnInit() {
    this.loadEvents();
  }

  loadEvents() {
    this.auditService.getAllEvents(0, 20).subscribe({
      next: (response) => {
        this.events = response.content || [];
        this.events.sort((a, b) =>
          new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
        );
      },
      error: (err) => console.error('Failed to load events', err)
    });
  }

  getEventClass(eventType: string): string {
    if (eventType.startsWith('ticket')) return 'ticket-event';
    if (eventType.startsWith('payment')) return 'payment-event';
    if (eventType.startsWith('user')) return 'user-event';
    return '';
  }

  getEventIcon(eventType: string): string {
    const icons: any = {
      'ticket.reserved': '🎫',
      'ticket.booked': '✅',
      'ticket.released': '❌',
      'payment.completed': '💳',
      'payment.failed': '⚠️',
      'user.registered': '👤'
    };
    return icons[eventType] || '📝';
  }
}
