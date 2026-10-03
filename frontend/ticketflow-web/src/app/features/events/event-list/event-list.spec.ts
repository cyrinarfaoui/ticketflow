import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { EventList } from './event-list';
import { AuthService } from '../../../core/services/auth.service';
import { EventResponse } from '../../../core/services/event.service';
import { API_BASE_URL } from '../../../core/config/api.config';

// Données de test : ce que le "faux serveur" renverra
const EVENTS: EventResponse[] = [
  {
    id: '1', title: 'Concert de jazz', description: 'Soirée jazz', location: 'Tunis', category: 'CONCERT',
    date: '2026-12-15T20:00:00', totalSeats: 100, availableSeats: 53,
  },
  {
    id: '2', title: 'Festival', description: 'Plein air', location: 'Tabarka', category: 'FESTIVAL',
    date: '2026-10-20T19:00:00', totalSeats: 250, availableSeats: 250,
  },
];

const EVENTS_URL = `${API_BASE_URL}/events`;

describe('EventList (intégration)', () => {
  let fixture: ComponentFixture<EventList>;
  let element: HTMLElement;
  let httpMock: HttpTestingController;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    // Horloge contrôlée (pour debounceTime)
    jasmine.clock().install();
    jasmine.clock().mockDate();

    // Faux AuthService : par défaut, l'utilisateur n'est PAS admin
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['hasRole']);
    authService.hasRole.and.returnValue(false);

    await TestBed.configureTestingModule({
      imports: [EventList],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    jasmine.clock().uninstall();
  });

  // Affiche la page, laisse passer le debounce, et renvoie la requête de recherche
  function openPage(): TestRequest {
    fixture = TestBed.createComponent(EventList);
    element = fixture.nativeElement;
    fixture.detectChanges();
    jasmine.clock().tick(300);
    return httpMock.expectOne((r) => r.url === EVENTS_URL);
  }

  function loadPage(events: EventResponse[] = EVENTS): void {
    openPage().flush(events);
    fixture.detectChanges();
  }

  // Simule la saisie dans un champ, puis la fin du debounce
  function type(selector: string, value: string): void {
    const input = element.querySelector<HTMLInputElement | HTMLSelectElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event(input.tagName === 'SELECT' ? 'change' : 'input'));
    jasmine.clock().tick(300);
  }

  function searchRequest(): TestRequest {
    return httpMock.expectOne((r) => r.url === EVENTS_URL);
  }

  describe('affichage', () => {

    it('ne fait qu\'une seule requête au chargement de la page', () => {
      loadPage();

      expect(element.querySelectorAll('app-event-card').length).toBe(2);
    });

    it('annonce le nombre de résultats', () => {
      loadPage();

      expect(element.querySelector('[role="status"]')?.textContent).toContain('2 événements trouvés');
    });

    it('affiche un message quand aucun événement ne correspond', () => {
      loadPage([]);

      expect(element.textContent).toContain('Aucun événement ne correspond à vos critères.');
    });
  });

  describe('droits (spy AuthService)', () => {

    it('cache le lien de création pour un utilisateur non admin', () => {
      loadPage();

      expect(element.querySelector('.create-link')).toBeNull();
      expect(authService.hasRole).toHaveBeenCalledWith('ADMIN');
    });

    it('affiche le lien de création pour un admin', () => {
      authService.hasRole.and.returnValue(true);

      loadPage();

      expect(element.querySelector('.create-link')).not.toBeNull();
    });
  });

  describe('filtres', () => {

    it('envoie le lieu saisi à l\'API après 300 ms', () => {
      loadPage();

      type('#filter-location', 'Tunis');

      const req = searchRequest();
      expect(req.request.params.get('location')).toBe('Tunis');
      req.flush([EVENTS[0]]);
      fixture.detectChanges();
      expect(element.querySelectorAll('app-event-card').length).toBe(1);
    });

    it('envoie la catégorie choisie', () => {
      loadPage();

      type('#filter-category', 'FESTIVAL');

      const req = searchRequest();
      expect(req.request.params.get('category')).toBe('FESTIVAL');
      req.flush([EVENTS[1]]);
    });

    it('envoie les dates de début et de fin', () => {
      loadPage();

      type('#filter-from', '2026-10-10');
      searchRequest().flush(EVENTS);
      type('#filter-to', '2026-10-31');

      const req = searchRequest();
      expect(req.request.params.get('from')).toBe('2026-10-10');
      expect(req.request.params.get('to')).toBe('2026-10-31');
      req.flush([EVENTS[1]]);
    });

    it('annule la recherche précédente quand une nouvelle arrive (switchMap)', () => {
      loadPage();

      type('#filter-location', 'Tu');
      const premiere = searchRequest();
      type('#filter-location', 'Tunis');
      const seconde = searchRequest();

      expect(premiere.cancelled).toBeTrue();
      expect(seconde.cancelled).toBeFalse();
      seconde.flush([EVENTS[0]]);
    });

    it('réinitialise tous les filtres', () => {
      loadPage();
      type('#filter-location', 'Tunis');
      searchRequest().flush([EVENTS[0]]);

      element.querySelector<HTMLButtonElement>('.filters__reset')!.click();
      jasmine.clock().tick(300);

      const req = searchRequest();
      expect(req.request.params.keys()).toEqual([]);
      req.flush(EVENTS);
    });
  });

  describe('URL partageable', () => {

    it('recopie les filtres dans l\'URL', async () => {
      loadPage();

      type('#filter-location', 'Tunis');
      searchRequest().flush([EVENTS[0]]);
      await fixture.whenStable();

      expect(TestBed.inject(Router).url).toBe('/?location=Tunis');
    });

    it('pré-remplit les filtres à partir d\'un lien partagé', async () => {
      await TestBed.inject(Router).navigateByUrl('/?location=Tunis&category=CONCERT&availableOnly=true');

      const req = openPage();

      expect(req.request.params.get('location')).toBe('Tunis');
      expect(req.request.params.get('category')).toBe('CONCERT');
      expect(req.request.params.get('availableOnly')).toBe('true');
      req.flush([EVENTS[0]]);
      fixture.detectChanges();
      expect(element.querySelector<HTMLInputElement>('#filter-location')!.value).toBe('Tunis');
    });
  });

  describe('réservation', () => {

    it('envoie POST /reservations puis relance la recherche avec les filtres actuels', () => {
      loadPage();
      type('#filter-location', 'Tunis');
      searchRequest().flush([EVENTS[0]]);
      fixture.detectChanges();

      element.querySelector<HTMLButtonElement>('app-event-card button')!.click();

      const reservation = httpMock.expectOne(`${API_BASE_URL}/reservations?eventId=1`);
      expect(reservation.request.method).toBe('POST');
      expect(reservation.request.body).toEqual({ seats: 1 });
      reservation.flush({ id: 'r1', eventId: '1', seats: 1, status: 'PENDING' });

      const refresh = searchRequest();
      expect(refresh.request.params.get('location')).toBe('Tunis');
      refresh.flush([EVENTS[0]]);
    });

    it('laisse le 401 à la bannière globale : aucun message dans la liste', () => {
      loadPage();

      element.querySelector<HTMLButtonElement>('app-event-card button')!.click();
      httpMock
        .expectOne(`${API_BASE_URL}/reservations?eventId=1`)
        .flush(null, { status: 401, statusText: 'Unauthorized' });
      fixture.detectChanges();

      expect(element.querySelector('[role="alert"]')).toBeNull();
    });

    it('affiche le message métier renvoyé par l\'API (409)', () => {
      loadPage();

      element.querySelector<HTMLButtonElement>('app-event-card button')!.click();
      httpMock
        .expectOne(`${API_BASE_URL}/reservations?eventId=1`)
        .flush({ error: 'Pas assez de places disponibles' }, { status: 409, statusText: 'Conflict' });
      fixture.detectChanges();

      expect(element.querySelector('[role="alert"]')?.textContent).toContain('Pas assez de places disponibles');
    });
  });
});
