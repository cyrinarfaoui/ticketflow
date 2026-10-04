import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReservationService } from './reservation.service';
import { API_BASE_URL } from '../config/api.config';

describe('ReservationService', () => {
  let service: ReservationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(ReservationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('crée une réservation avec POST /reservations?eventId=…', () => {
    service.createReservation('e1', 3).subscribe();

    const req = httpMock.expectOne(`${API_BASE_URL}/reservations?eventId=e1`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ seats: 3 });
    req.flush({});
  });

  it('annule une réservation avec POST /reservations/{id}/cancel', () => {
    service.cancelReservation('r1').subscribe();

    const req = httpMock.expectOne(`${API_BASE_URL}/reservations/r1/cancel`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});
