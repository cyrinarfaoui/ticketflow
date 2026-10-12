import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { EventResponse, EventService } from './event.service';
import { API_BASE_URL } from '../config/api.config';

const EVENTS_URL = `${API_BASE_URL}/events`;

const CONCERT: EventResponse = {
  id: 'e1', title: 'Concert de jazz', description: '', location: 'Tunis', category: 'CONCERT',
  date: '2026-12-15T20:00:00', totalSeats: 100, availableSeats: 53, status: 'OPEN', priceCents: 5000, currency: 'TND',
};

describe('EventService', () => {
  let service: EventService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(EventService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  // Vérifie qu'aucune requête inattendue n'est restée sans réponse
  afterEach(() => httpMock.verify());

  function searchRequest(): TestRequest {
    return httpMock.expectOne((request) => request.url === EVENTS_URL);
  }

  describe('search()', () => {

    it('n\'envoie aucun paramètre quand aucun filtre n\'est renseigné', () => {
      service.search({}).subscribe();

      const req = searchRequest();
      expect(req.request.method).toBe('GET');
      expect(req.request.params.keys()).toEqual([]);
      req.flush([]);
    });

    it('n\'envoie que les filtres renseignés (un champ vide = pas de filtre)', () => {
      service.search({
        title: '', location: 'Tunis', category: '', from: '2026-10-10', to: '', availableOnly: false,
      }).subscribe();

      const req = searchRequest();
      expect(req.request.params.keys()).toEqual(['location', 'from']);
      expect(req.request.params.get('location')).toBe('Tunis');
      req.flush([]);
    });

    it('envoie tous les filtres quand ils sont tous renseignés', () => {
      service.search({
        title: 'jazz', location: 'Tunis', category: 'CONCERT', from: '2026-10-10', to: '2026-10-31', availableOnly: true,
      }).subscribe();

      const req = searchRequest();
      expect(req.request.params.get('title')).toBe('jazz');
      expect(req.request.params.get('category')).toBe('CONCERT');
      expect(req.request.params.get('to')).toBe('2026-10-31');
      expect(req.request.params.get('availableOnly')).toBe('true');
      req.flush([]);
    });

    it('met à jour le signal events$ avec la réponse du serveur', () => {
      expect(service.events$()).toEqual([]);

      service.search({}).subscribe();
      searchRequest().flush([CONCERT]);

      expect(service.events$()).toEqual([CONCERT]);
    });
  });

  it('crée un événement avec POST /events', () => {
    const payload = {
      title: 'Concert de jazz', description: '', location: 'Tunis', category: 'CONCERT' as const,
      date: '2026-12-15T19:00:00.000Z', totalSeats: 100, priceCents: 5000,
    };

    service.createEvent(payload).subscribe();

    const req = httpMock.expectOne(EVENTS_URL);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(payload);
    req.flush(CONCERT);
  });
});
