import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { API_BASE_URL } from '../../../core/config/api.config';
import { MyReservation } from '../../../core/models/reservation';
import { EventResponse } from '../../../core/services/event.service';
import { PaymentDialog } from './payment-dialog';

registerLocaleData(localeFr);

const EVENT: EventResponse = {
  id: 'e1', title: 'Concert de jazz', description: '', location: 'Tunis', category: 'CONCERT',
  date: '2030-12-15T20:00:00', totalSeats: 100, availableSeats: 50, status: 'OPEN', priceCents: 5000, currency: 'TND',
};

describe('PaymentDialog', () => {
  let fixture: ComponentFixture<PaymentDialog>;
  let element: HTMLElement;
  let httpMock: HttpTestingController;
  let closedWith: boolean | undefined;

  beforeEach(() => {
    jasmine.clock().install();   // l'attente du résultat interroge l'API toutes les secondes : on contrôle le temps
    TestBed.configureTestingModule({
      imports: [PaymentDialog],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: LOCALE_ID, useValue: 'fr' }],
    });
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(PaymentDialog);
    fixture.componentRef.setInput('event', EVENT);
    fixture.componentRef.setInput('seats', 2);
    closedWith = undefined;
    fixture.componentInstance.closed.subscribe((created) => (closedWith = created));
    element = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => {
    httpMock.verify();
    jasmine.clock().uninstall();
  });

  function type(selector: string, value: string): void {
    const input = element.querySelector<HTMLInputElement>(selector)!;
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function fillCard(number: string, expiry = '12/30', cvc = '123'): void {
    type('#card-number', number);
    type('#card-expiry', expiry);
    type('#card-cvc', cvc);
  }

  function submit(): void {
    element.querySelector<HTMLButtonElement>('.pay-button')!.click();
    fixture.detectChanges();
  }

  // La saga répond : la réservation n'est plus "en attente"
  function bankAnswers(reservation: Partial<MyReservation>): void {
    jasmine.clock().tick(0);
    httpMock.expectOne(`${API_BASE_URL}/reservations/me`).flush([{ id: 'r1', status: 'PENDING', paymentFailureCode: null, ...reservation }]);
    fixture.detectChanges();
  }

  const text = () => element.textContent ?? '';

  // L'événement "close" d'un <dialog> natif arrive de façon asynchrone : on l'attend
  function closeAndWait(): Promise<Event> {
    const closing = new Promise<Event>((resolve) =>
      element.querySelector('dialog')!.addEventListener('close', resolve, { once: true }));
    fixture.componentInstance.close();
    return closing;
  }

  it('affiche le récapitulatif et le montant total', () => {
    expect(text()).toContain('2 place(s) × 50,00 TND');
    expect(element.querySelector('.recap__total')?.textContent).toContain('100,00 TND');
    expect(element.querySelector('.pay-button')?.textContent).toContain('Payer 100,00 TND');
  });

  it('formate le numéro de carte et la date pendant la frappe', () => {
    type('#card-number', '4242424242424242');
    type('#card-expiry', '1230');

    expect(element.querySelector<HTMLInputElement>('#card-number')!.value).toBe('4242 4242 4242 4242');
    expect(element.querySelector<HTMLInputElement>('#card-expiry')!.value).toBe('12/30');
  });

  it("n'envoie rien et affiche les erreurs si le formulaire est vide", () => {
    submit();

    expect(text()).toContain('Le numéro de carte est obligatoire.');
    expect(text()).toContain("La date d'expiration est obligatoire.");
    expect(text()).toContain('Le code de sécurité est obligatoire.');
    httpMock.expectNone(`${API_BASE_URL}/reservations?eventId=e1`);
  });

  it('détecte une faute de frappe dans le numéro (clé de Luhn)', () => {
    fillCard('4242 4242 4242 4241');
    submit();

    expect(text()).toContain('Numéro de carte invalide.');
    expect(element.querySelector('#card-number')?.getAttribute('aria-invalid')).toBe('true');
  });

  it('refuse une carte réelle en mode test, sans appeler l\'API', () => {
    fillCard('4111 1111 1111 1111');
    submit();

    expect(text()).toContain('Mode test : utilisez une des cartes de test ci-dessous.');
    httpMock.expectNone(`${API_BASE_URL}/reservations?eventId=e1`);
  });

  it("envoie le JETON et jamais le numéro de carte, puis affiche le paiement accepté", () => {
    fillCard('4242 4242 4242 4242');
    submit();

    const request = httpMock.expectOne(`${API_BASE_URL}/reservations?eventId=e1`);
    expect(request.request.body).toEqual({ seats: 2, paymentMethod: 'pm_card_visa' });
    expect(JSON.stringify(request.request.body)).not.toContain('4242');
    request.flush({ id: 'r1', eventId: 'e1', seats: 2, status: 'PENDING' });
    fixture.detectChanges();
    expect(text()).toContain('Vérification du paiement');

    bankAnswers({ status: 'CONFIRMED' });

    expect(text()).toContain('Paiement accepté : votre réservation est confirmée.');
  });

  it('affiche le motif du refus et permet de réessayer avec une autre carte', () => {
    fillCard('4000 0000 0000 9995');
    submit();
    httpMock.expectOne(`${API_BASE_URL}/reservations?eventId=e1`).flush({ id: 'r1', eventId: 'e1', seats: 2, status: 'PENDING' });

    bankAnswers({ status: 'CANCELLED', paymentFailureCode: 'insufficient_funds' });
    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Paiement refusé : fonds insuffisants');

    element.querySelector<HTMLButtonElement>('.state--failed .pay-button')!.click();
    fixture.detectChanges();
    expect(element.querySelector<HTMLInputElement>('#card-number')!.value).toBe('');
  });

  it("affiche l'erreur métier de l'API (ex. plus assez de places)", () => {
    fillCard('4242 4242 4242 4242');
    submit();

    httpMock.expectOne(`${API_BASE_URL}/reservations?eventId=e1`)
      .flush({ error: 'Pas assez de places disponibles' }, { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Pas assez de places disponibles');
  });

  it('"Voir mes réservations" ferme la fenêtre et ouvre la page Mes réservations', () => {
    const navigate = spyOn(TestBed.inject(Router), 'navigateByUrl').and.resolveTo(true);
    fillCard('4242 4242 4242 4242');
    submit();
    httpMock.expectOne(`${API_BASE_URL}/reservations?eventId=e1`).flush({ id: 'r1', eventId: 'e1', seats: 2, status: 'PENDING' });
    bankAnswers({ status: 'CONFIRMED' });

    element.querySelector<HTMLAnchorElement>('.state--success a')!.click();

    expect(navigate).toHaveBeenCalledWith('/mes-reservations');
    expect(element.querySelector<HTMLDialogElement>('dialog')!.open).toBeFalse();
  });

  it('prévient le parent qu\'une réservation a été créée quand on ferme après paiement', async () => {
    fillCard('4242 4242 4242 4242');
    submit();
    httpMock.expectOne(`${API_BASE_URL}/reservations?eventId=e1`).flush({ id: 'r1', eventId: 'e1', seats: 2, status: 'PENDING' });
    bankAnswers({ status: 'CONFIRMED' });

    await closeAndWait();

    expect(closedWith).toBeTrue();
  });

  it('fermer sans payer ne crée rien', async () => {
    await closeAndWait();

    expect(closedWith).toBeFalse();
  });
});
