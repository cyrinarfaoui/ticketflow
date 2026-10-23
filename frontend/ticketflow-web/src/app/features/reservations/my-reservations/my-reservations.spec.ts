import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { MyReservations } from './my-reservations';
import { AuthService } from '../../../core/services/auth.service';
import { API_BASE_URL } from '../../../core/config/api.config';
import { MyReservation } from '../../../core/models/reservation';

const ME_URL = `${API_BASE_URL}/reservations/me`;

function reservation(overrides: Partial<MyReservation> = {}): MyReservation {
  return {
    id: 'r1',
    eventId: 'e1',
    eventTitle: 'Concert de jazz',
    eventLocation: 'Tunis',
    eventDate: '2026-12-15T20:00:00',
    seats: 2,
    status: 'PENDING',
    createdAt: '2026-10-04T08:30:00Z',
    paymentFailureCode: null,
    eventCancelled: false,
    ...overrides,
  };
}

describe('MyReservations (intégration)', () => {
  let fixture: ComponentFixture<MyReservations>;
  let element: HTMLElement;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    const authService = jasmine.createSpyObj<AuthService>('AuthService', ['username']);
    authService.username.and.returnValue('cyrine');

    TestBed.configureTestingModule({
      imports: [MyReservations],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    });
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(MyReservations);
    element = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  async function respond(reservations: MyReservation[]): Promise<void> {
    httpMock.expectOne(ME_URL).flush(reservations);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function items(): HTMLElement[] {
    return [...element.querySelectorAll<HTMLElement>('.reservation')];
  }

  it('demande GET /reservations/me, sans identifiant d\'utilisateur dans l\'URL', () => {
    const req = httpMock.expectOne(ME_URL);

    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys()).toEqual([]);
    req.flush([]);
  });

  it('salue l\'utilisateur avec le nom lu dans le jeton (affichage uniquement)', async () => {
    await respond([]);

    expect(element.querySelector('.intro')?.textContent).toContain('Bonjour cyrine');
  });

  it('affiche chaque réservation avec son événement', async () => {
    await respond([reservation(), reservation({ id: 'r2', eventTitle: 'Festival', status: 'EXPIRED' })]);

    expect(items().length).toBe(2);
    expect(items()[0].querySelector('h2')?.textContent).toContain('Concert de jazz');
    expect(items()[0].textContent).toContain('Tunis');
    expect(items()[0].textContent).toContain('2 place(s)');
  });

  it('affiche un statut lisible avec une icône, pas seulement une couleur', async () => {
    await respond([
      reservation({ id: 'a', status: 'PENDING' }),
      reservation({ id: 'b', status: 'CONFIRMED' }),
      reservation({ id: 'c', status: 'CANCELLED' }),
      reservation({ id: 'd', status: 'EXPIRED' }),
    ]);

    const statuses = items().map((item) => item.querySelector('.status')!.textContent!.trim());
    expect(statuses).toEqual(['⏳ En attente de paiement', '✓ Confirmée', '✕ Annulée', '⌛ Expirée']);
    // L'icône est décorative : les lecteurs d'écran lisent seulement le texte
    expect(items()[0].querySelector('.status span')?.getAttribute('aria-hidden')).toBe('true');
  });

  it("ne propose jamais d'annuler : seul l'organisateur annule", async () => {
    await respond([reservation({ status: 'PENDING' }), reservation({ id: 'r2', status: 'CONFIRMED' })]);

    expect(element.querySelector('.cancel-button')).toBeNull();
    expect(element.textContent).not.toContain('Annuler');
  });

  it("affiche le motif d'un paiement refusé", async () => {
    await respond([reservation({ status: 'CANCELLED', paymentFailureCode: 'insufficient_funds' })]);

    expect(items()[0].querySelector('.payment-failure')?.textContent).toContain('Paiement refusé : fonds insuffisants');
  });

  it("affiche que l'événement a été annulé par l'organisateur", async () => {
    await respond([reservation({ status: 'CANCELLED', eventCancelled: true })]);

    expect(items()[0].querySelector('.payment-failure')?.textContent).toContain("Événement annulé par l'organisateur");
  });

  it('affiche un état vide avec un lien vers le catalogue', async () => {
    await respond([]);

    expect(element.textContent).toContain('Vous n\'avez pas encore de réservation.');
    expect(element.querySelector('.empty a')?.getAttribute('href')).toBe('/evenements');
  });

  it('affiche un message si le chargement échoue', async () => {
    httpMock.expectOne(ME_URL).flush(null, { status: 500, statusText: 'Server Error' });
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.textContent).toContain('Impossible de charger vos réservations.');
  });

});
