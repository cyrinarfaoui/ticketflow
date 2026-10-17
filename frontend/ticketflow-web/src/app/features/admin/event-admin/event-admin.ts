import { Component, computed, inject, linkedSignal, signal } from '@angular/core';
import { HttpErrorResponse, httpResource } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/config/api.config';
import { AdminService } from '../../../core/services/admin.service';
import { CreateEventPayload, EventResponse, EventService, EventStatus } from '../../../core/services/event.service';
import { EVENT_CATEGORIES, EventCategory } from '../../../core/models/event-category';
import { Page } from '../../../core/models/reservation';
import { DatePipe } from '@angular/common';
import { futureDate } from '../../../shared/validators/future-date.validator';
import { DialogService } from '../../../shared/dialog/dialog.service';

// 6 lignes compactes : une page entière tient dans le panneau, on change de page au lieu de défiler
export const EVENTS_PAGE_SIZE = 6;

/** Back-office des événements : la liste à gauche, l'événement choisi (modifiable) à droite. */
@Component({
  selector: 'app-event-admin',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './event-admin.html',
  styleUrl: './event-admin.scss',
})
export class EventAdmin {
  private readonly adminService = inject(AdminService);
  private readonly eventService = inject(EventService);
  private readonly dialog = inject(DialogService);
  protected readonly categories = EVENT_CATEGORIES;

  // 1. LES FILTRES ET LA PAGE : ce que l'admin a choisi
  protected readonly searchInput = signal('');                 // ce qui est tapé, à chaque frappe
  protected readonly status = signal<EventStatus | ''>('');    // '' = tous les statuts
  protected readonly page = signal(0);                         // première page = 0 (convention Spring)

  // On attend 300 ms de calme avant de chercher : pas une requête à chaque lettre tapée
  private readonly search = toSignal(
    toObservable(this.searchInput).pipe(debounceTime(300), distinctUntilChanged()),
    { initialValue: '' },
  );

  // 2. LA LISTE : UNE page, filtrée et triée PAR LE SERVEUR (même avec des milliers d'événements)
  //    httpResource relance la requête dès qu'un filtre ou la page change
  protected readonly events = httpResource<Page<EventResponse>>(() => {
    const params: Record<string, string | number> = { page: this.page(), size: EVENTS_PAGE_SIZE, sort: 'date,asc' };
    if (this.search()) params['title'] = this.search();
    if (this.status()) params['status'] = this.status();
    return { url: `${API_BASE_URL}/events/admin`, params };
  });

  // Pendant le chargement d'une nouvelle page, on garde l'ancienne affichée (pas de "clignotement")
  protected readonly currentPage = linkedSignal<Page<EventResponse> | undefined, Page<EventResponse> | undefined>({
    source: () => (this.events.hasValue() ? this.events.value() : undefined),
    computation: (current, previous) => current ?? previous?.value,
  });
  protected readonly list = computed(() => this.currentPage()?.content ?? []);

  // 3. L'ÉVÉNEMENT CHOISI : on retient son id, on retrouve l'événement dans la page affichée
  protected readonly selectedId = signal<string | null>(null);
  protected readonly selected = computed(() => this.list().find((e) => e.id === this.selectedId()) ?? null);

  // true = le panneau de droite affiche le formulaire de CRÉATION (le même formulaire, vide)
  protected readonly creating = signal(false);

  // Changer un filtre ramène à la première page
  onSearch(value: string): void {
    this.searchInput.set(value);
    this.page.set(0);
  }

  onStatusChange(value: string): void {
    this.status.set(value as EventStatus | '');
    this.page.set(0);
  }

  goToPage(page: number): void {
    this.page.set(page);
  }

  // 3. LES MESSAGES : succès ou erreur de la dernière action
  protected readonly message = signal<string | null>(null);

  // 4. LE FORMULAIRE (à droite)
  readonly form = inject(FormBuilder).nonNullable.group({
    title: ['', [Validators.required, Validators.minLength(3)]],
    description: [''],
    location: ['', Validators.required],
    category: ['' as EventCategory | '', Validators.required],
    date: ['', [Validators.required, futureDate()]],   // vérifié AVANT l'envoi (le serveur le vérifie aussi)
    totalSeats: [1, [Validators.required, Validators.min(1)]],
    price: [0, [Validators.required, Validators.min(0.01)]],
  });

  // Clic sur un événement de la liste : on remplit le formulaire avec ses valeurs
  // Bouton "Nouvel événement" : le panneau de droite passe en mode création (formulaire vide)
  startCreation(): void {
    this.selectedId.set(null);
    this.message.set(null);
    this.creating.set(true);
    this.form.reset({ title: '', description: '', location: '', category: '', date: '', totalSeats: 100, price: 50 });
    this.form.enable();
  }

  // Bouton "Retour" : on quitte la création sans rien enregistrer
  cancelCreation(): void {
    this.creating.set(false);
  }

  select(event: EventResponse): void {
    this.creating.set(false);
    this.selectedId.set(event.id);
    this.message.set(null);
    this.form.reset({
      title: event.title,
      description: event.description ?? '',
      location: event.location,
      category: event.category,
      date: event.date.slice(0, 16),     // "2026-12-15T20:00:00" → "2026-12-15T20:00" (format du champ date)
      totalSeats: event.totalSeats,
      price: event.priceCents / 100,     // centimes → TND
    });
    // Un événement annulé ne se modifie plus (règle du backend) : formulaire en lecture seule
    if (this.isReadOnly(event)) {
      this.form.disable();
    } else {
      this.form.enable();
    }
  }

  /** Un événement déjà passé (sa date est dépassée) : il a eu lieu, on ne le modifie plus. */
  protected isPast(event: EventResponse): boolean {
    return new Date(event.date).getTime() < Date.now();
  }

  /** Règles du backend reflétées dans l'interface : annulé ou passé = lecture seule. */
  protected isReadOnly(event: EventResponse): boolean {
    return event.status === 'CANCELLED' || this.isPast(event);
  }

  // Bouton "Créer l'événement" ou "Enregistrer" : POST en création, PUT en modification
  save(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const raw = this.form.getRawValue();
    const payload: CreateEventPayload = {
      title: raw.title,
      description: raw.description,
      location: raw.location,
      category: raw.category as EventCategory,
      date: `${raw.date}:00`,                  // heure locale telle que saisie (pas de décalage UTC)
      totalSeats: raw.totalSeats,
      priceCents: Math.round(raw.price * 100), // TND → centimes
    };
    const editing = this.selected();
    if (this.creating()) {
      this.run(this.eventService.createEvent(payload), `« ${payload.title} » créé.`, () => this.closePanel());
    } else if (editing) {
      this.run(this.adminService.updateEvent(editing.id, payload), `« ${payload.title} » enregistré.`, () => this.closePanel());
    }
  }

  // Enregistrement réussi : le formulaire se ferme, le message de succès reste affiché en haut
  private closePanel(): void {
    this.creating.set(false);
    this.selectedId.set(null);
  }

  // async/await : on ATTEND la réponse de la fenêtre de confirmation (une Promise) avant d'agir
  async close(event: EventResponse): Promise<void> {
    const confirmed = await this.dialog.confirm({
      title: 'Clôturer les réservations',
      message: `Plus personne ne pourra réserver « ${event.title} ». Les réservations existantes restent valables.`,
      confirmLabel: 'Clôturer',
    });
    if (confirmed) {
      this.run(this.adminService.closeEvent(event.id), 'Réservations clôturées.');
    }
  }

  async remove(event: EventResponse): Promise<void> {
    const confirmed = await this.dialog.confirm({
      title: 'Supprimer l\'événement',
      message: `« ${event.title} » sera supprimé définitivement. Cette action est irréversible.`,
      confirmLabel: 'Supprimer',
      tone: 'danger',
    });
    if (confirmed) {
      this.run(this.adminService.deleteEvent(event.id), `« ${event.title} » supprimé.`);
      this.selectedId.set(null);
    }
  }

  async cancelEvent(event: EventResponse): Promise<void> {
    const confirmed = await this.dialog.confirm({
      title: 'Annuler l\'événement',
      message: `« ${event.title} » n'aura pas lieu : toutes les réservations seront annulées et chaque client prévenu par e-mail.`,
      confirmLabel: 'Annuler l\'événement',
      tone: 'danger',
    });
    if (confirmed) {
      this.message.set('Annulation en cours…');
      this.run(this.adminService.cancelEventAndWait(event.id), `« ${event.title} » annulé, clients prévenus par e-mail.`);
    }
  }

  // La mécanique commune à toutes les actions : envoyer, puis afficher le message et recharger la liste
  private run<T>(request: Observable<T>, successMessage: string, onSuccess?: (result: T) => void): void {
    let result: T | undefined;
    request.subscribe({
      next: (value) => (result = value),
      complete: () => {
        if (onSuccess && result !== undefined) {
          onSuccess(result);
        }
        this.message.set(successMessage);   // après onSuccess : select() efface le message précédent
        this.events.reload();
      },
      error: (err: HttpErrorResponse) => {
        this.message.set(null);
        // L'erreur métier du serveur, annoncée dans une fenêtre (ex. "Cet événement a des réservations : annulez-le…")
        this.dialog.alert({ title: 'Action impossible', message: err.error?.error ?? "L'action a échoué. Réessayez." });
      },
    });
  }
}