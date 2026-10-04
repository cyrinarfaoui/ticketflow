import { Component, computed, inject, linkedSignal, signal } from '@angular/core';
import { DatePipe, PercentPipe } from '@angular/common';
import { HttpErrorResponse, httpResource } from '@angular/common/http';
import { ActivatedRoute, ParamMap, Params, Router } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { API_BASE_URL } from '../../../core/config/api.config';
import { AdminService } from '../../../core/services/admin.service';
import { EventResponse } from '../../../core/services/event.service';
import {
  AdminReservation,
  Page,
  RESERVATION_STATUSES,
  ReservationStats,
  ReservationStatus,
  reservationStatusLabel,
} from '../../../core/models/reservation';

type SortField = 'createdAt' | 'seats';
type SortDirection = 'asc' | 'desc';

// L'état du tableau, tel qu'il est lu dans l'URL (?status=PENDING&page=2&sort=seats,asc)
interface TableQuery {
  status: ReservationStatus | '';
  page: number;              // 1 = première page (lisible dans l'URL) ; l'API, elle, commence à 0
  sortField: SortField;
  sortDirection: SortDirection;
}

export const PAGE_SIZE = 10;

@Component({
  imports: [DatePipe, PercentPipe],
  selector: 'app-admin-dashboard',
  styleUrl: './admin-dashboard.scss',
  templateUrl: './admin-dashboard.html',
})
export class AdminDashboard {

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly adminService = inject(AdminService);

  protected readonly statuses = RESERVATION_STATUSES;
  protected readonly statusLabel = reservationStatusLabel;

  // 1. L'URL est la source de vérité : un lien partagé rouvre exactement la même vue
  private readonly queryParams = toSignal(this.route.queryParamMap, { requireSync: true });
  protected readonly query = computed(() => queryFromUrl(this.queryParams()));

  // 2. Lectures : httpResource relance la requête dès que query() change,
  //    et annule la précédente si elle n'est pas finie (comme switchMap)
  protected readonly stats = httpResource<ReservationStats>(() => `${API_BASE_URL}/reservations/stats`);

  protected readonly reservations = httpResource<Page<AdminReservation>>(() => ({
    url: `${API_BASE_URL}/reservations`,
    params: toApiParams(this.query()),
  }));

  // Quand la requête change (page, tri…), httpResource vide sa valeur le temps du chargement.
  // linkedSignal garde la page précédente affichée : le tableau ne "clignote" pas à chaque clic.
  protected readonly reservationPage = linkedSignal<Page<AdminReservation> | undefined, Page<AdminReservation> | undefined>({
    source: () => (this.reservations.hasValue() ? this.reservations.value() : undefined),
    computation: (current, previous) => current ?? previous?.value,
  });

  protected readonly events = httpResource<EventResponse[]>(() => `${API_BASE_URL}/events`, { defaultValue: [] });

  // L'API des réservations ne renvoie que l'eventId : on retrouve le titre dans la liste des événements
  private readonly eventTitles = computed(() => new Map(this.events.value().map((e) => [e.id, e.title])));

  // Message d'une erreur métier (409…) ; les erreurs techniques sont affichées par la bannière globale
  readonly actionError = signal<string | null>(null);

  eventTitle(eventId: string): string {
    return this.eventTitles().get(eventId) ?? 'Événement passé';
  }

  canCancel(reservation: AdminReservation): boolean {
    return reservation.status === 'PENDING' || reservation.status === 'CONFIRMED';
  }

  // ---------- Filtre, tri, pagination : on ne fait que modifier l'URL ----------

  onStatusChange(event: Event): void {
    const status = (event.target as HTMLSelectElement).value;
    // Changer de filtre ramène à la première page
    this.updateUrl({ status: status || null, page: null });
  }

  goToPage(page: number): void {
    this.updateUrl({ page: page > 1 ? page : null });
  }

  sortBy(field: SortField): void {
    const { sortField, sortDirection } = this.query();
    // Re-cliquer sur la même colonne inverse le sens ; une nouvelle colonne commence par "décroissant"
    const direction: SortDirection = field === sortField && sortDirection === 'desc' ? 'asc' : 'desc';
    this.updateUrl({ sort: `${field},${direction}`, page: null });
  }

  // Valeur de aria-sort : indique aux lecteurs d'écran la colonne triée et le sens
  ariaSort(field: SortField): 'ascending' | 'descending' | 'none' {
    const { sortField, sortDirection } = this.query();
    if (field !== sortField) return 'none';
    return sortDirection === 'asc' ? 'ascending' : 'descending';
  }

  // ---------- Actions admin ----------

  cancel(reservation: AdminReservation): void {
    if (!confirm(`Annuler cette réservation de ${reservation.seats} place(s) pour « ${this.eventTitle(reservation.eventId)} » ?`)) {
      return;
    }
    this.actionError.set(null);
    this.adminService.cancelReservation(reservation.id).subscribe({
      next: () => {
        // Le statut, les compteurs et les places restantes ont changé : on recharge les trois
        this.reservations.reload();
        this.stats.reload();
        this.events.reload();
      },
      error: (err: HttpErrorResponse) => this.actionError.set(err.error?.error ?? null),
    });
  }

  close(event: EventResponse): void {
    if (!confirm(`Clôturer les réservations de « ${event.title} » ? Plus personne ne pourra réserver.`)) {
      return;
    }
    this.actionError.set(null);
    this.adminService.closeEvent(event.id).subscribe({
      next: () => this.events.reload(),
      error: (err: HttpErrorResponse) => this.actionError.set(err.error?.error ?? null),
    });
  }

  private updateUrl(params: Params): void {
    // merge : on garde les autres paramètres ; null = on retire le paramètre de l'URL
    this.router.navigate([], { relativeTo: this.route, queryParams: params, queryParamsHandling: 'merge' });
  }
}

// ---------- Conversion URL <-> état <-> paramètres de l'API ----------

function queryFromUrl(params: ParamMap): TableQuery {
  const status = params.get('status');
  const page = Number(params.get('page'));
  const [field, direction] = (params.get('sort') ?? '').split(',');
  return {
    // On ne fait jamais confiance à l'URL : une valeur inconnue = valeur par défaut
    status: RESERVATION_STATUSES.some((s) => s.value === status) ? (status as ReservationStatus) : '',
    page: Number.isInteger(page) && page > 1 ? page : 1,
    sortField: field === 'seats' ? 'seats' : 'createdAt',
    sortDirection: direction === 'asc' ? 'asc' : 'desc',
  };
}

// Format attendu par Spring : ?page=0&size=10&sort=createdAt,desc&status=PENDING
function toApiParams(query: TableQuery): Record<string, string | number> {
  const params: Record<string, string | number> = {
    page: query.page - 1,
    size: PAGE_SIZE,
    sort: `${query.sortField},${query.sortDirection}`,
  };
  if (query.status) params['status'] = query.status;
  return params;
}
