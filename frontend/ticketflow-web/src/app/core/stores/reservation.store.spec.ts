import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReservationStore } from './reservation.store';
import { API_BASE_URL } from '../config/api.config';
import { MyReservation } from '../models/reservation';
import { LiveNotificationStore } from './live-notification.store';

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

  it("annonce au client une réservation qui a changé d'état depuis le dernier chargement", () => {
    const notifyReservation = spyOn(TestBed.inject(LiveNotificationStore), 'notifyReservation');
    store.load();
    httpMock.expectOne(ME_URL).flush([reservation('r1', 'PENDING'), reservation('r2', 'CONFIRMED')]);

    store.load();
    const expired = reservation('r1', 'EXPIRED');
    httpMock.expectOne(ME_URL).flush([expired, reservation('r2', 'CONFIRMED')]);

    expect(notifyReservation).toHaveBeenCalledOnceWith(expired);   // r2 n'a pas changé : rien
  });

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

  it('se vide à la déconnexion', () => {
    loadWith([reservation('r1', 'PENDING')]);

    store.clear();

    expect(store.reservations()).toEqual([]);
    expect(store.status()).toBe('idle');
  });
});