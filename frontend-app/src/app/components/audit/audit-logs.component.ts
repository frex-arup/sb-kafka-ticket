import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatTableModule } from '@angular/material/table';
import { AuditService } from '../../services/audit.service';

@Component({
  selector: 'app-audit-logs',
  standalone: true,
  imports: [CommonModule, FormsModule, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule, MatTableModule],
  template: `
    <div class="container">
      <h1>📝 Audit Logs - Event Sourcing</h1>
      <p>Complete trail of all events stored in MongoDB</p>

      <mat-card>
        <mat-card-content>
          <mat-form-field appearance="outline" style="width: 100%;">
            <mat-label>Search by Correlation ID (trace complete flow)</mat-label>
            <input matInput [(ngModel)]="correlationId" (keyup.enter)="searchByCorrelation()">
          </mat-form-field>
          <button mat-raised-button color="primary" (click)="searchByCorrelation()">
            Search
          </button>
          <button mat-raised-button (click)="loadAllEvents()" style="margin-left: 10px;">
            Show All
          </button>
        </mat-card-content>
      </mat-card>

      <mat-card *ngIf="logs.length > 0" style="margin-top: 20px;">
        <mat-card-header>
          <mat-card-title>Audit Logs ({{ logs.length }})</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <div *ngFor="let log of logs" class="log-entry">
            <div class="log-header">
              <span class="event-type">{{ log.eventType }}</span>
              <span class="timestamp">{{ log.timestamp | date:'medium' }}</span>
            </div>
            <div class="log-details">
              <span><strong>Event ID:</strong> {{ log.eventId }}</span>
              <span><strong>Correlation ID:</strong> {{ log.correlationId }}</span>
              <span><strong>Topic:</strong> {{ log.topic }}</span>
            </div>
            <details>
              <summary style="cursor: pointer; color: #3f51b5;">View Full Event</summary>
              <pre class="event-data">{{ log.eventData | json }}</pre>
            </details>
          </div>
        </mat-card-content>
      </mat-card>

      <mat-card style="margin-top: 20px; background: #fff3e0;">
        <mat-card-header>
          <mat-card-title>💡 Event Sourcing Benefits</mat-card-title>
        </mat-card-header>
        <mat-card-content>
          <ul>
            <li><strong>Complete Audit Trail:</strong> Every state change is recorded</li>
            <li><strong>Event Replay:</strong> Can rebuild system state from events</li>
            <li><strong>Debugging:</strong> Trace issues by examining event history</li>
            <li><strong>Compliance:</strong> Maintain records for regulatory requirements</li>
            <li><strong>Analytics:</strong> Analyze business processes from event data</li>
          </ul>
        </mat-card-content>
      </mat-card>
    </div>
  `,
  styles: [`
    .container { padding: 20px; }
    .log-entry { border-left: 3px solid #3f51b5; padding: 15px; margin: 10px 0; background: #f5f5f5; border-radius: 4px; }
    .log-header { display: flex; justify-content: space-between; margin-bottom: 10px; }
    .event-type { font-weight: bold; color: #3f51b5; }
    .timestamp { color: #666; font-size: 0.9em; }
    .log-details { display: flex; gap: 20px; margin-bottom: 10px; font-size: 0.9em; }
    .event-data { background: white; padding: 10px; border-radius: 4px; overflow-x: auto; }
  `]
})
export class AuditLogsComponent implements OnInit {
  correlationId = '';
  logs: any[] = [];

  constructor(private auditService: AuditService) {}

  ngOnInit() {
    this.loadAllEvents();
  }

  loadAllEvents() {
    this.auditService.getAllEvents(0, 50).subscribe({
      next: (response) => {
        this.logs = response.content || [];
        this.logs.sort((a, b) =>
          new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime()
        );
      },
      error: (err) => console.error('Failed to load audit logs', err)
    });
  }

  searchByCorrelation() {
    if (!this.correlationId) {
      this.loadAllEvents();
      return;
    }

    this.auditService.getEventsByCorrelationId(this.correlationId).subscribe({
      next: (logs) => {
        this.logs = logs;
        this.logs.sort((a, b) =>
          new Date(a.timestamp).getTime() - new Date(b.timestamp).getTime()
        );
      },
      error: (err) => console.error('Failed to search audit logs', err)
    });
  }
}
