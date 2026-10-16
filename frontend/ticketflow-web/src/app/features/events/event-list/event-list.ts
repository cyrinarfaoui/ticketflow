import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, ParamMap, Params, Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, combineLatest, debounceTime, distinctUntilChanged, map, startWith, switchMap, tap } from 'rxjs';
import { EventResponse, EventService } from '../../../core/services/event.service';
import { ReservationStore } from '../../../core/stores/reservation.store';
import { AuthService } from '../../../core/services/auth.service';
import { EVENT_CATEGORIES, EventCategory } from '../../../core/models/event-category';
import { EventCard } from '../event-card/event-card';
import { PaymentDialog } from '../payment-dialog/payment-dialog';

// Les filtres du catalogue, tels qu'ils sont dans le formulaire et dans l'URL
interface CatalogFilters {
  title: string;
  location: string;
  category: EventCategory | '';
  from: string;
  to: string;
  availableOnly: boolean;
}

@Component({
  imports: [RouterLink, ReactiveFormsModule, EventCard, PaymentDialog],
  selector: 'app-event-list',
  styleUrl: './event-list.scss',
  templateUrl: './event-list.html',
})
export class EventList implements OnInit {

  protected readonly authService = inject(AuthService);
  protected readonly eventService = inject(EventService);
  private readonly reservationStore = inject(ReservationStore);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  protected readonly categories = EVENT_CATEGORIES;
  // La commande en cours de paiement (null = fenêtre de paiement fermée)
  readonly checkout = signal<{ event: EventResponse; seats: number } | null>(null);

  // Formulaire des filtres : la source de vérité, recopiée dans l'URL à chaque changement
  readonly filters = inject(FormBuilder).nonNullable.group({
    title: '',
    location: '',
    category: '' as EventCategory | '',
    from: '',
    to: '',
    availableOnly: false,
  });

  // Relance la recherche avec les filtres actuels (par exemple après une réservation)
  private readonly refresh$ = new Subject<void>();

  ngOnInit(): void {
    // 1. Un lien partagé (/evenements?location=Tunis...) pré-remplit les filtres
    this.filters.setValue(filtersFromUrl(this.route.snapshot.queryParamMap), { emitEvent: false });

    combineLatest([
      // 2. Les filtres : on attend 300 ms de calme, et on ignore les valeurs identiques
      this.filters.valueChanges.pipe(
        startWith(null),
        debounceTime(300),
        map(() => this.filters.getRawValue()),
        distinctUntilChanged(sameFilters),
      ),
      // 3. Les demandes de rafraîchissement
      this.refresh$.pipe(startWith(undefined)),
    ]).pipe(
      tap(([filters]) => this.updateUrl(filters)),
      // 4. switchMap : une nouvelle recherche ANNULE la précédente encore en cours
      switchMap(([filters]) =>
        this.eventService.search(filters).pipe(
          // une erreur (affichée par la bannière globale) ne doit pas arrêter les recherches suivantes
          catchError(() => EMPTY),
        ),
      ),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe();
  }

  resetFilters(): void {
    this.filters.reset();
  }

  reserve(event: EventResponse, seats: number): void {
    // Comme sur un vrai site : on se connecte AVANT de payer
    if (!this.authService.isLoggedIn()) {
      this.authService.login();
      return;
    }
    this.checkout.set({ event, seats });
  }

  // Fenêtre de paiement fermée : si une réservation a été créée, on met à jour places et badge
  onCheckoutClosed(reservationCreated: boolean): void {
    this.checkout.set(null);
    if (reservationCreated) {
      this.refresh$.next();          // places restantes à jour dans le catalogue
      this.reservationStore.load();  // badge "Mes réservations" à jour dans le header
    }
  }

  // Recopie les filtres dans l'URL, sans créer une entrée d'historique à chaque frappe
  private updateUrl(filters: CatalogFilters): void {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: toQueryParams(filters),
      replaceUrl: true,
    });
  }
}

function filtersFromUrl(params: ParamMap): CatalogFilters {
  const category = params.get('category');
  return {
    title: params.get('title') ?? '',
    location: params.get('location') ?? '',
    category: EVENT_CATEGORIES.some((c) => c.value === category) ? (category as EventCategory) : '',
    from: params.get('from') ?? '',
    to: params.get('to') ?? '',
    availableOnly: params.get('availableOnly') === 'true',
  };
}

// Seuls les filtres renseignés apparaissent dans l'URL
function toQueryParams(filters: CatalogFilters): Params {
  const params: Params = {};
  if (filters.title) params['title'] = filters.title;
  if (filters.location) params['location'] = filters.location;
  if (filters.category) params['category'] = filters.category;
  if (filters.from) params['from'] = filters.from;
  if (filters.to) params['to'] = filters.to;
  if (filters.availableOnly) params['availableOnly'] = 'true';
  return params;
}

function sameFilters(a: CatalogFilters, b: CatalogFilters): boolean {
  return a.title === b.title
    && a.location === b.location
    && a.category === b.category
    && a.from === b.from
    && a.to === b.to
    && a.availableOnly === b.availableOnly;
}
