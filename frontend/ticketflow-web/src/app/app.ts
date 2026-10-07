import { Component, effect, inject, untracked } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { ReservationStore } from './core/stores/reservation.store';
import { NotificationBanner } from './shared/notification-banner/notification-banner';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NotificationBanner],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly authService = inject(AuthService);
  protected readonly reservationStore = inject(ReservationStore);

  constructor() {
    // Connexion → on charge les réservations (pour le badge) ; déconnexion → on vide le store.
    // effect() se relance tout seul à chaque changement de isLoggedIn().
    effect(() => {
      const loggedIn = this.authService.isLoggedIn();
      untracked(() => (loggedIn ? this.reservationStore.load() : this.reservationStore.clear()));
    });
  }
}
