import { inject, Injectable, signal } from '@angular/core';
import { API_BASE_URL } from '../config/api.config';
import { EVENT_SOURCE_FACTORY } from '../config/event-source';
import { LiveNotification, StreamStatus } from '../models/live-notification';
import { EventService } from '../services/event.service';

/**
 * Les notifications en direct (Server-Sent Events) : Kafka → notification-service (WebFlux) → ce store → l'interface.
 * Les composants LISENT les signals ; seul le store les MODIFIE.
 */
@Injectable({ providedIn: 'root' })
export class LiveNotificationStore {

  static readonly MAX_ITEMS = 10;           // on ne garde que les plus récentes
  static readonly RETRY_DELAY_MS = 5000;    // délai avant de rouvrir un flux que le navigateur a abandonné

  private readonly createEventSource = inject(EVENT_SOURCE_FACTORY);
  private readonly eventService = inject(EventService);

  private readonly items = signal<LiveNotification[]>([]);
  private readonly unread = signal(0);
  private readonly streamStatus = signal<StreamStatus>('connecting');

  readonly notifications = this.items.asReadonly();
  readonly unreadCount = this.unread.asReadonly();
  readonly status = this.streamStatus.asReadonly();

  private source: EventSource | null = null;
  private retryTimer: ReturnType<typeof setTimeout> | null = null;

  /** Ouvre le flux (sans effet s'il est déjà ouvert). */
  connect(): void {
    if (this.source || this.retryTimer) {
      return;
    }
    this.streamStatus.set('connecting');
    this.open();
  }

  /** Ferme le flux et annule toute reconnexion prévue. */
  disconnect(): void {
    if (this.retryTimer) {
      clearTimeout(this.retryTimer);
      this.retryTimer = null;
    }
    this.source?.close();
    this.source = null;
  }

  /** L'utilisateur a ouvert la liste : plus rien de "nouveau". */
  markAllRead(): void {
    this.unread.set(0);
  }

  private open(): void {
    const source = this.createEventSource(`${API_BASE_URL}/notifications/stream`);
    this.source = source;

    source.onopen = () => this.streamStatus.set('open');

    // 'reservation' = le nom d'événement SSE choisi côté serveur (ServerSentEvent.event("reservation"))
    source.addEventListener('reservation', (message) => this.receive((message as MessageEvent<string>).data));

    source.onerror = () => {
      this.streamStatus.set('reconnecting');
      // État CONNECTING : le navigateur se reconnecte TOUT SEUL (reconnexion intégrée à EventSource).
      // État CLOSED : il a abandonné (réponse HTTP en erreur…) → c'est à nous de réessayer plus tard.
      if (source.readyState === EventSource.CLOSED) {
        this.source = null;
        this.retryTimer = setTimeout(() => {
          this.retryTimer = null;
          this.open();
        }, LiveNotificationStore.RETRY_DELAY_MS);
      }
    };
  }

  private receive(data: string): void {
    let notification: LiveNotification;
    try {
      notification = JSON.parse(data) as LiveNotification;
    } catch {
      return;   // message illisible : on l'ignore plutôt que de casser le flux
    }

    // La plus récente en premier, et pas plus de MAX_ITEMS
    this.items.update((list) => [notification, ...list].slice(0, LiveNotificationStore.MAX_ITEMS));
    this.unread.update((count) => count + 1);

    // Le catalogue affiché se met à jour tout seul : "41 places restantes" sans recharger la page
    if (notification.availableSeats !== null) {
      this.eventService.updateAvailableSeats(notification.eventId, notification.availableSeats);
    }
  }
}
