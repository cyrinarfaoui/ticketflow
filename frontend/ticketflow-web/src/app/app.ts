import { Component, DestroyRef, effect, inject, untracked } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';
import { ReservationStore } from './core/stores/reservation.store';
import { LiveNotificationStore } from './core/stores/live-notification.store';
import { LiveNotifications } from './shared/live-notifications/live-notifications';
import { DialogHost } from './shared/dialog/dialog-host';
import { NotificationBanner } from './shared/notification-banner/notification-banner';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive, NotificationBanner, LiveNotifications, DialogHost],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly authService = inject(AuthService);
  protected readonly reservationStore = inject(ReservationStore);
  private readonly liveNotifications = inject(LiveNotificationStore);

  constructor() {
    // Flux public d'activité en direct : ouvert dès le démarrage, fermé quand l'application est détruite
    this.liveNotifications.connect();
    inject(DestroyRef).onDestroy(() => this.liveNotifications.disconnect());

    // Connexion → on charge les réservations (pour le badge) ; déconnexion → on vide le store.
    // effect() se relance tout seul à chaque changement de isLoggedIn().
    effect(() => {
      const loggedIn = this.authService.isLoggedIn();
      untracked(() => (loggedIn ? this.reservationStore.load() : this.reservationStore.clear()));
    });

    // La cloche : activité publique pour l'administrateur, suivi de ses réservations pour le client
    effect(() => this.liveNotifications.setPublicActivity(this.authService.isAdmin()));
  }

  // Avec le routeur Angular, un simple href="#contenu" changerait de page : on déplace le focus nous-mêmes
  protected skipToContent(event: Event): void {
    event.preventDefault();
    document.getElementById('contenu')?.focus();
  }
}
