import { ComponentFixture, TestBed } from '@angular/core/testing';
import { EventCard } from './event-card';
import { EventResponse } from '../../../core/services/event.service';

// FABRIQUE : crée un événement de test valide, dont on peut changer certains champs
function createEvent(overrides: Partial<EventResponse> = {}): EventResponse {
  return {
    id: '1',
    title: 'Concert de jazz',
    description: 'Soirée jazz',
    location: 'Tunis',
    date: '2026-12-15T20:00:00',
    totalSeats: 100,
    availableSeats: 53,
    ...overrides,
  };
}

describe('EventCard', () => {
  let fixture: ComponentFixture<EventCard>;
  let component: EventCard;
  let element: HTMLElement;

  // Donne un événement à la carte puis met à jour le HTML
  function render(event: EventResponse): void {
    fixture.componentRef.setInput('event', event);
    fixture.detectChanges();
  }

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EventCard],
    }).compileComponents();

    fixture = TestBed.createComponent(EventCard);
    component = fixture.componentInstance;
    element = fixture.nativeElement;
  });

  describe('logique (computed)', () => {

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
  });

  describe('interaction', () => {

    it('émet le nombre de places choisi au clic sur Réserver', () => {
      // Arrange
      render(createEvent());
      let emitted: number | undefined;
      component.reserve.subscribe((seats) => (emitted = seats));

      // Act : l'utilisateur tape 3 puis clique
      const input = element.querySelector<HTMLInputElement>('input[type="number"]')!;
      input.value = '3';
      input.dispatchEvent(new Event('input'));
      element.querySelector<HTMLButtonElement>('button')!.click();

      // Assert
      expect(emitted).toBe(3);
    });
  });
});