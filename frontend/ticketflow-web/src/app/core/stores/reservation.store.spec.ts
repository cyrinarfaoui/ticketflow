import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReservationStore } from './reservation.store';
import { API_BASE_URL } from '../config/api.config';
import { MyReservation } from '../models/reservation';

const ME_URL = `${API_BASE_URL}/reservations/me`;

function reservation(id: string, status: MyReservation['status']): MyReservation {
  return {
    id, eventId: 'e1', eventTitle: 'Concert', eventLocation: 'Tunis',
    eventDate: '2026-12-15T20:00:00', seats: 1, status, createdAt: '2026-10-04T08:00:00Z', paymentFailureCode: null, eventCancelled: false,
  };
}

describe('ReservationStore', () => {
  let store: ReservationStore;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    store = TestBed.inject(ReservationStore);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  function loadWith(reservations: MyReservation[]): void {
    store.load();
    httpMock.expectOne(ME_URL).flush(reservations);
  }

  it('est vide au départ', () => {
    expect(store.reservations()).toEqual([]);
    expect(store.activeCount()).toBe(0);
    expect(store.status()).toBe('idle');
  });

  it('charge les réservations et passe par l\'état "loading"', () => {
    store.load();
    expect(store.status()).toBe('loading');

    httpMock.expectOne(ME_URL).flush([reservation('r1', 'PENDING')]);

    expect(store.status()).toBe('loaded');
    expect(store.reservations().length).toBe(1);
  });

  it('compte seulement les réservations actives (en attente ou confirmées)', () => {
    loadWith([
      reservation('r1', 'PENDING'),
      reservation('r2', 'CONFIRMED'),
      reservation('r3', 'CANCELLED'),
      reservation('r4', 'EXPIRED'),
    ]);

    expect(store.activeCount()).toBe(2);
  });

  it('passe à l\'état "error" si le chargement échoue', () => {
    store.load();
    httpMock.expectOne(ME_URL).flush(null, { status: 500, statusText: 'Server Error' });

    expect(store.status()).toBe('error');
  });

  describe('annulation optimiste', () => {

    it('met l\'écran à jour AVANT la réponse du serveur', () => {
      loadWith([reservation('r1', 'PENDING')]);

      store.cancel('r1').subscribe();

      // La requête n'a pas encore répondu, mais l'état a déjà changé
      expect(store.reservations()[0].status).toBe('CANCELLED');
      expect(store.activeCount()).toBe(0);
      httpMock.expectOne(`${API_BASE_URL}/reservations/r1/cancel`).flush({});
      expect(store.reservations()[0].status).toBe('CANCELLED');
    });

    it('revient en arrière si le serveur refuse', () => {
      loadWith([reservation('r1', 'PENDING')]);

      store.cancel('r1').subscribe({ error: () => { /* le composant affichera le message */ } });
      httpMock
        .expectOne(`${API_BASE_URL}/reservations/r1/cancel`)
        .flush({ error: 'Déjà annulée' }, { status: 409, statusText: 'Conflict' });

      expect(store.reservations()[0].status).toBe('PENDING');   // rollback
      expect(store.activeCount()).toBe(1);
    });
  });

  it('se vide à la déconnexion', () => {
    loadWith([reservation('r1', 'PENDING')]);

    store.clear();

    expect(store.reservations()).toEqual([]);
    expect(store.status()).toBe('idle');
  });
});