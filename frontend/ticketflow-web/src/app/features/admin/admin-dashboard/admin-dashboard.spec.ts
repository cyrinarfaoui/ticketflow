import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { NavigationEnd, provideRouter, Router } from '@angular/router';
import { filter, firstValueFrom } from 'rxjs';
import { AdminDashboard } from './admin-dashboard';
import { API_BASE_URL } from '../../../core/config/api.config';
import { EventResponse } from '../../../core/services/event.service';
import { AdminReservation, Page, ReservationStats } from '../../../core/models/reservation';

const STATS: ReservationStats = {
  createdToday: 3, total: 10, pending: 2, confirmed: 6, cancelled: 0, expired: 2, expirationRate: 0.2,
};

const EVENTS: EventResponse[] = [
  {
    id: 'e1', title: 'Concert de jazz', description: '', location: 'Tunis', category: 'CONCERT',
    date: '2026-12-15T20:00:00', totalSeats: 100, availableSeats: 53, status: 'OPEN',
  },
  {
    id: 'e2', title: 'Festival', description: '', location: 'Tabarka', category: 'FESTIVAL',
    date: '2026-11-20T19:00:00', totalSeats: 250, availableSeats: 10, status: 'CLOSED',
  },
];

const PENDING: AdminReservation = { id: 'r1', eventId: 'e1', seats: 2, status: 'PENDING', createdAt: '2026-10-04T08:30:00Z' };
// eventId inconnu : un événement passé, absent de GET /events
const CANCELLED: AdminReservation = { id: 'r2', eventId: 'old', seats: 1, status: 'CANCELLED', createdAt: '2026-10-01T10:00:00Z' };

function page(content: AdminReservation[], totalPages = 1): Page<AdminReservation> {
  return { content, page: 0, size: 10, totalElements: content.length, totalPages };
}

describe('AdminDashboard (intégration)', () => {
  let fixture: ComponentFixture<AdminDashboard>;
  let element: HTMLElement;
  let httpMock: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AdminDashboard],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    httpMock = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  afterEach(() => httpMock.verify());

  // Ouvre la page à une URL donnée (pour tester les liens partagés)
  async function open(url = '/'): Promise<void> {
    await router.navigateByUrl(url);
    fixture = TestBed.createComponent(AdminDashboard);
    element = fixture.nativeElement;
    fixture.detectChanges(); // httpResource envoie les 3 requêtes
  }

  function reservationsRequest(): TestRequest {
    return httpMock.expectOne((r) => r.url === `${API_BASE_URL}/reservations`);
  }

  // Le faux serveur répond aux 3 requêtes du dashboard
  async function respond(reservations = page([PENDING, CANCELLED]), events = EVENTS): Promise<void> {
    httpMock.expectOne(`${API_BASE_URL}/reservations/stats`).flush(STATS);
    reservationsRequest().flush(reservations);
    httpMock.expectOne(`${API_BASE_URL}/events`).flush(events);
    await render();
  }

  async function render(): Promise<void> {
    await fixture.whenStable();
    fixture.detectChanges();
  }

  // Une action qui modifie l'URL : on attend la fin de la navigation,
  // puis TestBed.tick() propage les signaux → httpResource envoie la nouvelle requête
  async function navigateBy(action: () => void): Promise<void> {
    const navigated = firstValueFrom(router.events.pipe(filter((e) => e instanceof NavigationEnd)));
    action();
    await navigated;
    TestBed.tick();
  }

  function button(label: string): HTMLButtonElement {
    return [...element.querySelectorAll<HTMLButtonElement>('button')].find((b) => b.textContent?.includes(label))!;
  }

  function rows(): HTMLTableRowElement[] {
    return [...element.querySelectorAll<HTMLTableRowElement>('section[aria-labelledby="reservations-title"] tbody tr')];
  }

  describe('indicateurs', () => {

    it('affiche les réservations du jour et le taux d\'expiration', async () => {
      await open();
      await respond();

      const stats = element.querySelector('.stats')!.textContent!;
      expect(stats).toContain('3');
      expect(stats).toContain('réservation(s) aujourd\'hui');
      expect(stats).toContain('20');
      expect(stats).toContain('2 expirée(s)');
    });
  });

  describe('erreurs de chargement', () => {

    it('affiche un message au lieu d\'une section vide quand l\'API échoue', async () => {
      await open();
      httpMock.expectOne(`${API_BASE_URL}/reservations/stats`).flush(null, { status: 404, statusText: 'Not Found' });
      reservationsRequest().flush(null, { status: 404, statusText: 'Not Found' });
      httpMock.expectOne(`${API_BASE_URL}/events`).flush(EVENTS);
      await render();

      expect(element.textContent).toContain('Impossible de charger les indicateurs.');
      expect(element.textContent).toContain('Impossible de charger les réservations.');
    });
  });

  describe('tableau des réservations', () => {

    it('demande la première page, triée par date décroissante', async () => {
      await open();

      const req = reservationsRequest();
      expect(req.request.params.get('page')).toBe('0');
      expect(req.request.params.get('size')).toBe('10');
      expect(req.request.params.get('sort')).toBe('createdAt,desc');
      expect(req.request.params.has('status')).toBeFalse();
      req.flush(page([]));
      httpMock.expectOne(`${API_BASE_URL}/reservations/stats`).flush(STATS);
      httpMock.expectOne(`${API_BASE_URL}/events`).flush(EVENTS);
    });

    it('affiche le titre de l\'événement, le statut en clair et le bouton Annuler si possible', async () => {
      await open();
      await respond();

      expect(rows().length).toBe(2);
      expect(rows()[0].textContent).toContain('Concert de jazz');
      expect(rows()[0].textContent).toContain('En attente');
      expect(rows()[0].querySelector('button')?.textContent).toContain('Annuler');
      expect(rows()[1].textContent).toContain('Événement passé');
      expect(rows()[1].querySelector('button')).toBeNull(); // déjà annulée
    });

    it('affiche un message quand aucune réservation ne correspond', async () => {
      await open();
      await respond(page([]));

      expect(rows()[0].textContent).toContain('Aucune réservation ne correspond à ce filtre.');
    });

    it('ne fait pas confiance à l\'URL : des valeurs invalides donnent les valeurs par défaut', async () => {
      await open('/?status=PIRATE&page=-5&sort=password,drop');

      const req = reservationsRequest();
      expect(req.request.params.has('status')).toBeFalse();          // statut inconnu ignoré
      expect(req.request.params.get('page')).toBe('0');              // page négative → première page
      expect(req.request.params.get('sort')).toBe('createdAt,desc'); // tri inconnu → tri par défaut
      req.flush(page([]));
      httpMock.expectOne(`${API_BASE_URL}/reservations/stats`).flush(STATS);
      httpMock.expectOne(`${API_BASE_URL}/events`).flush(EVENTS);
    });

    it('trie par places et inverse le sens au second clic', async () => {
      await open();
      await respond();

      await navigateBy(() => button('Places').click());
      expect(reservationsRequest().request.params.get('sort')).toBe('seats,desc');

      await navigateBy(() => button('Places').click());
      const req = httpMock.match((r) => r.url === `${API_BASE_URL}/reservations`);
      expect(req[req.length - 1].request.params.get('sort')).toBe('seats,asc');
      req.forEach((r) => r.flush(page([PENDING])));
    });

    it('reprend filtre, page et tri depuis un lien partagé', async () => {
      await open('/?status=CONFIRMED&page=2&sort=seats,asc');

      const req = reservationsRequest();
      expect(req.request.params.get('status')).toBe('CONFIRMED');
      expect(req.request.params.get('page')).toBe('1'); // page 2 dans l'URL = page 1 pour Spring
      expect(req.request.params.get('sort')).toBe('seats,asc');
      req.flush(page([], 3));
      httpMock.expectOne(`${API_BASE_URL}/reservations/stats`).flush(STATS);
      httpMock.expectOne(`${API_BASE_URL}/events`).flush(EVENTS);
      await render();

      expect(element.querySelector<HTMLSelectElement>('#status-filter')!.value).toBe('CONFIRMED');
      expect(element.textContent).toContain('Page 2 sur 3');
    });

    it('filtre par statut et revient à la première page', async () => {
      await open('/?page=3');
      await respond(page([PENDING], 5));

      const select = element.querySelector<HTMLSelectElement>('#status-filter')!;
      await navigateBy(() => {
        select.value = 'PENDING';
        select.dispatchEvent(new Event('change'));
      });

      const req = reservationsRequest();
      expect(req.request.params.get('status')).toBe('PENDING');
      expect(req.request.params.get('page')).toBe('0');
      expect(router.url).toBe('/?status=PENDING');
      req.flush(page([PENDING]));
    });

    it('inverse le tri au clic sur l\'en-tête Date et l\'annonce avec aria-sort', async () => {
      await open();
      await respond();
      const th = element.querySelector('th[aria-sort]')!;
      expect(th.getAttribute('aria-sort')).toBe('descending');

      await navigateBy(() => button('Date').click());

      const req = reservationsRequest();
      expect(req.request.params.get('sort')).toBe('createdAt,asc');
      req.flush(page([CANCELLED, PENDING]));
      await render();
      expect(th.getAttribute('aria-sort')).toBe('ascending');
    });

    it('garde la page affichée pendant le chargement de la suivante', async () => {
      await open();
      await respond(page([PENDING], 3));

      await navigateBy(() => button('Suivante').click());
      fixture.detectChanges();

      // La requête est partie mais pas encore revenue : l'ancienne page reste visible, estompée
      expect(rows()[0].textContent).toContain('Concert de jazz');
      expect(element.querySelector('.table-wrapper')?.getAttribute('aria-busy')).toBe('true');
      reservationsRequest().flush(page([CANCELLED], 3));
    });

    it('passe à la page suivante (pagination côté serveur)', async () => {
      await open();
      await respond(page([PENDING], 3));
      expect(element.textContent).toContain('Page 1 sur 3');
      expect(button('Précédente').disabled).toBeTrue();

      await navigateBy(() => button('Suivante').click());

      const req = reservationsRequest();
      expect(req.request.params.get('page')).toBe('1');
      expect(router.url).toBe('/?page=2');
      req.flush(page([CANCELLED], 3));
    });
  });

  describe('actions admin', () => {

    it('annule une réservation après confirmation, puis recharge tout', async () => {
      spyOn(window, 'confirm').and.returnValue(true);
      await open();
      await respond();

      rows()[0].querySelector('button')!.click();

      const cancel = httpMock.expectOne(`${API_BASE_URL}/reservations/r1/cancel`);
      expect(cancel.request.method).toBe('POST');
      cancel.flush({ ...PENDING, status: 'CANCELLED' });
      TestBed.tick();

      // Statut, compteurs et places restantes ont changé : les 3 lectures repartent
      httpMock.expectOne(`${API_BASE_URL}/reservations/stats`).flush(STATS);
      reservationsRequest().flush(page([{ ...PENDING, status: 'CANCELLED' }, CANCELLED]));
      httpMock.expectOne(`${API_BASE_URL}/events`).flush(EVENTS);
    });

    it('n\'annule rien si l\'admin refuse la confirmation', async () => {
      spyOn(window, 'confirm').and.returnValue(false);
      await open();
      await respond();

      rows()[0].querySelector('button')!.click();

      expect(window.confirm).toHaveBeenCalled();
      httpMock.expectNone(`${API_BASE_URL}/reservations/r1/cancel`);
    });

    it('affiche le message métier renvoyé par l\'API (409)', async () => {
      spyOn(window, 'confirm').and.returnValue(true);
      await open();
      await respond();

      rows()[0].querySelector('button')!.click();
      httpMock
        .expectOne(`${API_BASE_URL}/reservations/r1/cancel`)
        .flush({ error: 'Cette réservation est déjà annulée' }, { status: 409, statusText: 'Conflict' });
      await render();

      expect(element.querySelector('[role="alert"]')?.textContent).toContain('Cette réservation est déjà annulée');
    });

    it('propose de clôturer uniquement les événements ouverts', async () => {
      spyOn(window, 'confirm').and.returnValue(true);
      await open();
      await respond();

      const closeButtons = [...element.querySelectorAll('button')].filter((b) => b.textContent?.includes('Clôturer'));
      expect(closeButtons.length).toBe(1); // le Festival est déjà clôturé

      closeButtons[0].click();
      const close = httpMock.expectOne(`${API_BASE_URL}/events/e1/close`);
      expect(close.request.method).toBe('POST');
      close.flush({ ...EVENTS[0], status: 'CLOSED' });
      TestBed.tick();

      httpMock.expectOne(`${API_BASE_URL}/events`).flush([{ ...EVENTS[0], status: 'CLOSED' }, EVENTS[1]]);
    });
  });
});
