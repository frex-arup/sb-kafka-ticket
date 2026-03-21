import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuditLog } from '../models/ticket.model';

@Injectable({ providedIn: 'root' })
export class AuditService {
  private apiUrl = `${environment.auditServiceUrl}/audit`;

  constructor(private http: HttpClient) {}

  getAllEvents(page: number = 0, size: number = 50): Observable<any> {
    return this.http.get(`${this.apiUrl}?page=${page}&size=${size}`);
  }

  getEventsByCorrelationId(correlationId: string): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.apiUrl}/correlation/${correlationId}`);
  }

  getEventsByType(eventType: string): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.apiUrl}/type/${eventType}`);
  }
}
