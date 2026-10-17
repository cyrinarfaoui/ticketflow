import { accessibleText } from '../../../../testing/accessible-text';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { FeaturedEvents } from './featured-events';
import { EventResponse } from '../../../core/services/event.service';
import { API_BASE_URL } from '../../../core/config/api.config';
import { provideRouter } from '@angular/router';

function event(id: string, date: string, availableSeats = 10): EventResponse {
  return {
    id,
    title: `Événement ${id}`,
    description: '',
    location: 'Tunis',
    category: 'CONCERT',
    date,
    totalSeats: 100,
    availableSeats,
    status: 'OPEN', priceCents: 5000, currency: 'TND',
  };
}

describe('FeaturedEvents', () => {
  let fixture: ComponentFixture<FeaturedEvents>;
  let element: HTMLElement;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    // "Maintenant" = le 27 septembre 2026 à midi, pour que les dates des tests restent valables
    jasmine.clock().install();
    jasmine.clock().mockDate(new Date('2026-09-27T12:00:00'));

    TestBed.configureTestingModule({
      imports: [FeaturedEvents],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(FeaturedEvents);
    element = fixture.nativeElement;
    fixture.detectChanges();   // le composant s'affiche et httpResource lance la requête
  });

  afterEach(() => {
    httpMock.verify();
    jasmine.clock().uninstall();
  });

  // Le faux serveur répond, puis on attend qu'Angular ait mis la page à jour
  async function respondWith(events: EventResponse[]): Promise<void> {
    httpMock.expectOne(`${API_BASE_URL}/events`).flush(events);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function titles(): string[] {
    return [...element.querySelectorAll('h3')].map((h3) => h3.textContent!.trim());
  }

  it('a un titre "À la une"', () => {
    httpMock.expectOne(`${API_BASE_URL}/events`);

    expect(element.querySelector('h2')?.textContent?.trim()).toBe('À la une');
  });

  it('affiche un message pendant le chargement', () => {
    httpMock.expectOne(`${API_BASE_URL}/events`);

    expect(element.querySelector('[role="status"]')?.textContent).toContain('Chargement');
  });

  it('affiche les 3 prochains événements disponibles, du plus proche au plus lointain', async () => {
    await respondWith([
      event('D', '2026-10-04T20:00:00'),
      event('passé', '2026-09-01T20:00:00'),
      event('A', '2026-10-01T20:00:00'),
      event('complet', '2026-10-02T20:00:00', 0),
      event('C', '2026-10-03T20:00:00'),
      event('B', '2026-10-02T21:00:00'),
    ]);

    expect(titles()).toEqual(['Événement A', 'Événement B', 'Événement C']);
  });

  it('affiche des cartes sans bouton de réservation', async () => {
    await respondWith([event('A', '2026-10-01T20:00:00')]);

    expect(element.querySelector('app-event-card')).not.toBeNull();
    expect(element.querySelector('button')).toBeNull();
  });

  it('affiche un message quand aucun événement n\'est à venir', async () => {
    await respondWith([]);

    expect(element.textContent).toContain('Aucun événement à venir');
  });

  it('affiche une alerte si le chargement échoue', async () => {
    httpMock
      .expectOne(`${API_BASE_URL}/events`)
      .flush('Erreur serveur', { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Impossible de charger');
  });

  it('propose un lien vers tous les événements', () => {
    httpMock.expectOne(`${API_BASE_URL}/events`);

    const link = element.querySelector<HTMLAnchorElement>('.featured__all');
    expect(accessibleText(link)).toBe('Voir tous les événements');
    expect(link?.getAttribute('href')).toBe('/evenements');
  });
});