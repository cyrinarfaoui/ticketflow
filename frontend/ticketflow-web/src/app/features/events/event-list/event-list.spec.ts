import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { EventList } from './event-list';
import { AuthService } from '../../../core/services/auth.service';
import { EventResponse } from '../../../core/services/event.service';
import { API_BASE_URL } from '../../../core/config/api.config';

// Données de test : ce que le "faux serveur" renverra
const EVENTS: EventResponse[] = [
  {
    id: '1', title: 'Concert de jazz', description: 'Soirée jazz', location: 'Tunis',
    date: '2026-12-15T20:00:00', totalSeats: 100, availableSeats: 53,
  },
  {
    id: '2', title: 'Festival', description: 'Plein air', location: 'Tabarka',
    date: '2026-10-02T19:00:00', totalSeats: 250, availableSeats: 250,
  },
];

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
    fixture = TestBed.createComponent(EventList);
    element = fixture.nativeElement;
  });

  afterEach(() => {
    httpMock.verify();
    jasmine.clock().uninstall();
  });

  // Simule le chargement complet de la page
  function loadPage(events: EventResponse[] = EVENTS): void {
    fixture.detectChanges();                                           // ngOnInit → loadEvents()
    httpMock.expectOne(`${API_BASE_URL}/events`).flush(events);        // 1re requête
    jasmine.clock().tick(300);                                         // fin du debounceTime
    httpMock.expectOne(`${API_BASE_URL}/events`).flush(events);        // 2e requête (recherche vide)
    fixture.detectChanges();                                           // met à jour le HTML
  }

  describe('affichage', () => {

    it('affiche une carte par événement reçu de l\'API', () => {
      loadPage();

      expect(element.querySelectorAll('app-event-card').length).toBe(2);
    });

    it('affiche un message quand il n\'y a aucun événement', () => {
      loadPage([]);

      expect(element.textContent).toContain('Aucun événement');
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

  describe('recherche', () => {

    it('envoie le lieu saisi à l\'API après 300 ms', () => {
      loadPage();

      const input = element.querySelector<HTMLInputElement>('#search-location')!;
      input.value = 'Tunis';
      input.dispatchEvent(new Event('input'));
      jasmine.clock().tick(300);

      const req = httpMock.expectOne(
        (r) => r.url === `${API_BASE_URL}/events` && r.params.get('location') === 'Tunis'
      );
      req.flush([EVENTS[0]]);
      fixture.detectChanges();

      expect(element.querySelectorAll('app-event-card').length).toBe(1);
    });
  });

  describe('réservation', () => {

    it('envoie POST /reservations puis recharge les événements', () => {
      loadPage();

      element.querySelector<HTMLButtonElement>('app-event-card button')!.click();

      const req = httpMock.expectOne(`${API_BASE_URL}/reservations?eventId=1`);
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ seats: 1 });
      req.flush({ id: 'r1', eventId: '1', seats: 1, status: 'PENDING' });

      httpMock.expectOne(`${API_BASE_URL}/events`).flush(EVENTS);
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