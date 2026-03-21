import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-movie-list',
  standalone: true,
  imports: [CommonModule, RouterModule, MatCardModule, MatButtonModule],
  template: `
    <div class="container">
      <h1>🎬 Now Showing</h1>
      <div class="movies-grid">
        <mat-card *ngFor="let movie of movies">
          <mat-card-header>
            <mat-card-title>{{ movie.title }}</mat-card-title>
            <mat-card-subtitle>{{ movie.genre }} • {{ movie.duration }}</mat-card-subtitle>
          </mat-card-header>
          <mat-card-content>
            <p>{{ movie.description }}</p>
            <p><strong>Show Times:</strong> {{ movie.showTimes.join(', ') }}</p>
            <p><strong>Price:</strong> $ {{ movie.price }}</p>
          </mat-card-content>
          <mat-card-actions>
            <button mat-raised-button color="primary" routerLink="/reserve"
                    [queryParams]="{movie: movie.title, price: movie.price}">
              Book Tickets
            </button>
          </mat-card-actions>
        </mat-card>
      </div>
    </div>
  `,
  styles: [`
    .container { padding: 20px; }
    .movies-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(300px, 1fr)); gap: 20px; margin-top: 20px; }
  `]
})
export class MovieListComponent {
  movies = [
    { title: 'Inception', genre: 'Sci-Fi', duration: '2h 28m', price: 15,
      description: 'A thief who steals corporate secrets through dream-sharing technology.',
      showTimes: ['10:00 AM', '2:00 PM', '6:00 PM', '9:30 PM'] },
    { title: 'The Dark Knight', genre: 'Action', duration: '2h 32m', price: 15,
      description: 'When the menace known as the Joker wreaks havoc on Gotham.',
      showTimes: ['11:00 AM', '3:00 PM', '7:00 PM', '10:00 PM'] },
    { title: 'Interstellar', genre: 'Sci-Fi', duration: '2h 49m', price: 18,
      description: 'A team of explorers travel through a wormhole in space.',
      showTimes: ['12:00 PM', '4:00 PM', '8:00 PM'] },
    { title: 'The Matrix', genre: 'Sci-Fi', duration: '2h 16m', price: 12,
      description: 'A computer hacker learns about the true nature of reality.',
      showTimes: ['10:30 AM', '1:30 PM', '5:30 PM', '9:00 PM'] }
  ];
}
