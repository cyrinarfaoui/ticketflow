import { inject, Injectable, signal } from '@angular/core';
import { API_BASE_URL } from '../config/api.config';
import { EVENT_SOURCE_FACTORY } from '../config/event-source';
import { BellNotification, LiveNotification, StreamStatus } from '../models/live-notification';
import { MyReservation } from '../models/reservation';
import { declineLabel } from '../models/payment';
import { EventService } from '../services/event.service';

/**
 * Les notifications de la cloche, selon le profil :
 * - administrateur : l'activité publique en direct (Server-Sent Events : Kafka → notification-service → ce store) ;
 * - client : le suivi de SES réservations (confirmée, refusée, expirée, annulée par l'organisateur).
 * Le flux SSE reste ouvert pour tout le monde : il met à jour les places restantes du catalogue.
 * Les composants LISENT les signals ; seul le store les MODIFIE.
 */
@Injectable({ providedIn: 'root' })
export class LiveNotificationStore {

  static readonly MAX_ITEMS = 10;           // on ne garde que les plus récentes
  static readonly RETRY_DELAY_MS = 5000;    // délai avant de rouvrir un flux que le navigateur a abandonné

  private readonly createEventSource = inject(EVENT_SOURCE_FACTORY);
  private readonly eventService = inject(EventService);

  private readonly items = signal<BellNotification[]>([]);
  private readonly unread = signal(0);
  private readonly streamStatus = signal<StreamStatus>('connecting');
  private readonly publicActivity = signal(false);

  readonly notifications = this.items.asReadonly();
  readonly unreadCount = this.unread.asReadonly();
  readonly status = this.streamStatus.asReadonly();
  /** true : la cloche affiche l'activité publique (admin) ; false : le suivi des réservations (client). */
  readonly showsPublicActivity = this.publicActivity.asReadonly();

  private source: EventSource | null = null;
  private retryTimer: ReturnType<typeof setTimeout> | null = null;
  // "id:statut" déjà annoncés : un même changement n'est jamais notifié deux fois
  private readonly announced = new Set<string>();

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

  /** L'administrateur voit l'activité publique ; un client voit seulement ses réservations. */
  setPublicActivity(visible: boolean): void {
    if (visible !== this.publicActivity()) {
      this.publicActivity.set(visible);
      this.items.set([]);
      this.unread.set(0);
    }
  }

  /** Annonce au client le nouvel état de SA réservation (une seule fois par état). */
  notifyReservation(reservation: MyReservation): void {
    const message = reservationMessage(reservation);
    const key = `${reservation.id}:${reservation.status}`;
    if (!message || this.announced.has(key)) {
      return;
    }
    this.announced.add(key);
    this.add({ message });
  }

  /** L'utilisateur a ouvert la liste : plus rien de "nouveau". */
  markAllRead(): void {
    this.unread.set(0);
  }

  private add(notification: BellNotification): void {
    // La plus récente en premier, et pas plus de MAX_ITEMS
    this.items.update((list) => [notification, ...list].slice(0, LiveNotificationStore.MAX_ITEMS));
    this.unread.update((count) => count + 1);
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

    if (this.publicActivity()) {
      this.add(notification);
    }

    // Pour tout le monde : le catalogue affiché se met à jour tout seul, sans recharger la page
    if (notification.availableSeats !== null) {
      this.eventService.updateAvailableSeats(notification.eventId, notification.availableSeats);
    }
  }
}

function seatsLabel(seats: number): string {
  return seats === 1 ? '1 place' : `${seats} places`;
}

// Le message affiché au client pour chaque état de sa réservation
function reservationMessage(reservation: MyReservation): string | null {
  const title = `« ${reservation.eventTitle} »`;
  switch (reservation.status) {
    case 'PENDING':
      return `Votre demande de réservation pour ${title} (${seatsLabel(reservation.seats)}) est en attente de paiement.`;
    case 'CONFIRMED':
      return `Votre réservation pour ${title} (${seatsLabel(reservation.seats)}) est confirmée. Votre billet vous a été envoyé par e-mail.`;
    case 'EXPIRED':
      return `Votre réservation pour ${title} a expiré : le paiement n'a pas été reçu à temps.`;
    case 'CANCELLED':
      if (reservation.paymentFailureCode) {
        return `Votre réservation pour ${title} n'a pas abouti. ${declineLabel(reservation.paymentFailureCode)}.`;
      }
      if (reservation.eventCancelled) {
        return `${title} a été annulé par l'organisateur. Votre réservation est annulée et le remboursement est automatique.`;
      }
      return `Votre réservation pour ${title} a été annulée par l'organisateur.`;
    default:
      return null;
  }
}
