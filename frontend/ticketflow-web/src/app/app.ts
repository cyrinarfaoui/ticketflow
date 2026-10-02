import { Component, inject, signal } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { NotificationBanner } from './shared/notification-banner/notification-banner';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NotificationBanner],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('ticketflow-web');
  protected authService = inject(AuthService);
}