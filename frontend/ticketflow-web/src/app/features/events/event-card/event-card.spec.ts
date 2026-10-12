import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { EventCard } from './event-card';
import { EventResponse } from '../../../core/services/event.service';
import { ReserveRequest } from '../../../core/models/payment';

// FABRIQUE : crée un événement de test valide, dont on peut changer certains champs
function createEvent(overrides: Partial<EventResponse> = {}): EventResponse {
  return {
    id: '1',
    title: 'Concert de jazz',
    description: 'Soirée jazz',
    location: 'Tunis',
    category: 'CONCERT',
    date: '2026-12-15T20:00:00',
    totalSeats: 100,
    availableSeats: 53,
    status: 'OPEN', priceCents: 5000, currency: 'TND',
    ...overrides,
  };
}

describe('EventCard', () => {
  let fixture: ComponentFixture<EventCard>;
  let component: EventCard;
  let element: HTMLElement;

  // Charge une seule fois les données françaises (mois, jours…)
  beforeAll(() => registerLocaleData(localeFr));

  // Donne un événement à la carte puis met à jour le HTML
  function render(event: EventResponse): void {
    fixture.componentRef.setInput('event', event);
    fixture.detectChanges();
  }

  // Texte d'un élément, sans les espaces autour
  function text(selector: string): string | undefined {
    return element.querySelector(selector)?.textContent?.trim();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EventCard],
      providers: [{ provide: LOCALE_ID, useValue: 'fr' }],
    }).compileComponents();

    fixture = TestBed.createComponent(EventCard);
    component = fixture.componentInstance;
    element = fixture.nativeElement;
  });

  describe('logique (computed)', () => {

    it('cache la zone de réservation quand la carte n\'est pas réservable', () => {
      fixture.componentRef.setInput('bookable', false);
      render(createEvent());

      expect(element.querySelector('.reserve-row')).toBeNull();
    });

    it('calcule le pourcentage de places restantes', () => {
      render(createEvent({ totalSeats: 100, availableSeats: 53 }));

      expect(component.fillPercent()).toBe(53);
    });

    it('est "available" quand il reste plus de 10 % des places', () => {
      render(createEvent({ totalSeats: 100, availableSeats: 11 }));

      expect(component.status()).toBe('available');
    });

    it('est "few" quand il reste 10 % des places ou moins', () => {
      render(createEvent({ totalSeats: 250, availableSeats: 15 }));

      expect(component.status()).toBe('few');
    });

    it('est "full" quand il ne reste aucune place', () => {
      render(createEvent({ availableSeats: 0 }));

      expect(component.status()).toBe('full');
    });
  });

  describe('affichage (HTML)', () => {

    it('affiche la catégorie en clair', () => {
      render(createEvent({ category: 'THEATRE' }));

      expect(text('.event-category')).toBe('Théâtre');
    });

    it('affiche le titre et le lieu', () => {
      render(createEvent());

      expect(element.querySelector('h3')?.textContent).toContain('Concert de jazz');
      expect(element.textContent).toContain('Tunis');
    });

    it('affiche "Complet" et cache le bouton quand il ne reste aucune place', () => {
      render(createEvent({ availableSeats: 0 }));

      expect(element.textContent).toContain('Complet');
      expect(element.querySelector('button')).toBeNull();
    });

    it('signale un événement clôturé et cache le bouton Réserver', () => {
      render(createEvent({ status: 'CLOSED' }));

      expect(text('.seats-label--closed')).toBe('Réservations clôturées');
      expect(element.querySelector('button')).toBeNull();
    });
  });

  describe('interaction', () => {

    it('émet le nombre de places et la carte choisis au clic sur Réserver', () => {
      // Arrange
      render(createEvent());
      let emitted: ReserveRequest | undefined;
      component.reserve.subscribe((request) => (emitted = request));

      // Act : l'utilisateur tape 3 puis clique
      const input = element.querySelector<HTMLInputElement>('input[type="number"]')!;
      input.value = '3';
      input.dispatchEvent(new Event('input'));
      element.querySelector<HTMLButtonElement>('button')!.click();

      // Assert : Visa par défaut
      expect(emitted).toEqual({ seats: 3, paymentMethod: 'pm_card_visa' });
    });

    it('envoie le JETON de la carte de test choisie, jamais un numéro de carte', () => {
      render(createEvent());
      let emitted: ReserveRequest | undefined;
      component.reserve.subscribe((request) => (emitted = request));

      const select = element.querySelector<HTMLSelectElement>('select')!;
      select.value = 'pm_card_insufficientFunds';
      select.dispatchEvent(new Event('change'));
      element.querySelector<HTMLButtonElement>('.reserve-row button')!.click();

      expect(emitted?.paymentMethod).toBe('pm_card_insufficientFunds');
    });

    it("affiche le prix d'une place et le total à payer", () => {
      render(createEvent({ priceCents: 2550 }));

      const input = element.querySelector<HTMLInputElement>('input[type="number"]')!;
      input.value = '2';
      input.dispatchEvent(new Event('input'));
      fixture.detectChanges();

      expect(element.querySelector('.event-price')?.textContent).toContain('25,50 TND');
      expect(element.querySelector('.reserve-total')?.textContent).toContain('51,00 TND');
    });
  });

  describe('date', () => {

    beforeEach(() => render(createEvent({ date: '2026-12-15T20:00:00' })));

    it('affiche le jour et le mois abrégé en français dans le badge', () => {
      expect(text('.date-badge__day')).toBe('15');
      expect(text('.date-badge__month')).toBe('déc.');
    });

    it('affiche le jour de la semaine et l\'heure', () => {
      expect(text('.event-time')).toBe('mar. 20:00');
    });

    it('donne la date complète aux lecteurs d\'écran', () => {
      expect(text('.date-badge .visually-hidden')).toBe('mardi 15 décembre 2026 à 20:00');
    });

    it('fournit la date au format machine dans l\'attribut datetime', () => {
      expect(element.querySelector('time')?.getAttribute('datetime')).toBe('2026-12-15T20:00:00');
    });
  });
});