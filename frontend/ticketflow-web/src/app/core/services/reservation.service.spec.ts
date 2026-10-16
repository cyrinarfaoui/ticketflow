import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReservationService } from './reservation.service';
import { API_BASE_URL } from '../config/api.config';
import { TimeoutError } from 'rxjs';
import { MyReservation } from '../models/reservation';

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
    service.createReservation('e1', 3, 'pm_card_visa').subscribe();

    const req = httpMock.expectOne(`${API_BASE_URL}/reservations?eventId=e1`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ seats: 3, paymentMethod: 'pm_card_visa' });
    req.flush({});
  });

  it('annule une réservation avec POST /reservations/{id}/cancel', () => {
    service.cancelReservation('r1').subscribe();

    const req = httpMock.expectOne(`${API_BASE_URL}/reservations/r1/cancel`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  describe('attente du résultat du paiement (saga asynchrone)', () => {
    const ME_URL = `${API_BASE_URL}/reservations/me`;
    const mine = (status: MyReservation['status']) => [{ id: 'r1', status } as MyReservation];

    beforeEach(() => jasmine.clock().install());
    afterEach(() => jasmine.clock().uninstall());

    it("redemande toutes les secondes jusqu'à un résultat définitif, puis s'arrête", () => {
      let result: MyReservation | undefined;
      service.waitForPaymentResult('r1').subscribe((r) => (result = r));

      jasmine.clock().tick(0);
      httpMock.expectOne(ME_URL).flush(mine('PENDING'));   // pas encore : la banque réfléchit
      expect(result).toBeUndefined();

      jasmine.clock().tick(ReservationService.POLL_INTERVAL_MS);
      httpMock.expectOne(ME_URL).flush(mine('CONFIRMED'));

      expect(result?.status).toBe('CONFIRMED');
      jasmine.clock().tick(ReservationService.POLL_INTERVAL_MS * 3);
      httpMock.expectNone(ME_URL);                          // take(1) : plus aucune requête
    });

    it("abandonne après le délai maximal (TimeoutError)", () => {
      let error: unknown;
      service.waitForPaymentResult('r1').subscribe({ error: (e) => (error = e) });

      for (let elapsed = 0; elapsed < ReservationService.RESULT_TIMEOUT_MS; elapsed += ReservationService.POLL_INTERVAL_MS) {
        jasmine.clock().tick(elapsed === 0 ? 0 : ReservationService.POLL_INTERVAL_MS);
        httpMock.match(ME_URL).forEach((req) => req.flush(mine('PENDING')));
      }
      jasmine.clock().tick(ReservationService.POLL_INTERVAL_MS);
      httpMock.match(ME_URL).forEach((req) => req.flush(mine('PENDING')));

      expect(error).toBeInstanceOf(TimeoutError);
    });
  });
});
