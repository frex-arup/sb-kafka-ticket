import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { Ticket, TicketRequest } from '../models/ticket.model';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private apiUrl = `${environment.ticketServiceUrl}/tickets`;

  constructor(private http: HttpClient) {}

  reserveTicket(request: TicketRequest): Observable<Ticket> {
    return this.http.post<Ticket>(`${this.apiUrl}/reserve`, request);
  }

  getTicketById(id: string): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.apiUrl}/${id}`);
  }

  getUserTickets(userId: string): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(`${this.apiUrl}/user/${userId}`);
  }

  getAllTickets(): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(this.apiUrl);
  }
}
