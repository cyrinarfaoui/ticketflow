import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminService } from './admin.service';
import { API_BASE_URL } from '../config/api.config';

describe('AdminService', () => {
  let service: AdminService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(AdminService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('annule une réservation avec POST /reservations/{id}/cancel', () => {
    service.cancelReservation('r1').subscribe();

    const req = httpMock.expectOne(`${API_BASE_URL}/reservations/r1/cancel`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });

  it('clôture un événement avec POST /events/{id}/close', () => {
    service.closeEvent('e1').subscribe();

    const req = httpMock.expectOne(`${API_BASE_URL}/events/e1/close`);
    expect(req.request.method).toBe('POST');
    req.flush({});
  });
});
