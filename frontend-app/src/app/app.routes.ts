import { Routes } from '@angular/router';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { MovieListComponent } from './components/movies/movie-list.component';
import { TicketReservationComponent } from './components/booking/ticket-reservation.component';
import { MyBookingsComponent } from './components/profile/my-bookings.component';
import { EventMonitorComponent } from './components/events/event-monitor.component';
import { AuditLogsComponent } from './components/audit/audit-logs.component';

export const routes: Routes = [
  { path: '', component: DashboardComponent },
  { path: 'movies', component: MovieListComponent },
  { path: 'reserve', component: TicketReservationComponent },
  { path: 'bookings', component: MyBookingsComponent },
  { path: 'events', component: EventMonitorComponent },
  { path: 'audit', component: AuditLogsComponent },
  { path: '**', redirectTo: '' }
];
