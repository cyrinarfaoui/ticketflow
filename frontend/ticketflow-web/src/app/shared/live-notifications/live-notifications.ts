import { Component, computed, inject, signal } from '@angular/core';
import { LiveNotificationStore } from '../../core/stores/live-notification.store';

/** La cloche de l'en-tête : badge des nouveautés, liste de l'activité en direct, annonce aux lecteurs d'écran. */
@Component({
  selector: 'app-live-notifications',
  templateUrl: './live-notifications.html',
  styleUrl: './live-notifications.scss',
  host: { '(keydown.escape)': 'close()' },   // Échap referme la liste (RGAA : tout est faisable au clavier)
})
export class LiveNotifications {
  protected readonly store = inject(LiveNotificationStore);
  protected readonly expanded = signal(false);

  // La dernière notification, lue par les lecteurs d'écran (zone aria-live)
  protected readonly latest = computed(() => this.store.notifications()[0]?.message ?? '');

  toggle(): void {
    const next = !this.expanded();
    this.expanded.set(next);
    if (next) {
      this.store.markAllRead();
    }
  }

  close(): void {
    this.expanded.set(false);
  }
}
