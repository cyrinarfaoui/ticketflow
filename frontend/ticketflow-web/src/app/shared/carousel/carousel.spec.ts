import { Component } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Carousel } from './carousel';

// Un hôte de test : 10 cartes de 300 px dans une zone de 600 px → ça déborde, les boutons apparaissent
@Component({
  imports: [Carousel],
  template: `
    <div style="width: 600px">
      <app-carousel label="Événements" trackId="test-track">
        <ul class="carousel-list" role="list" style="display: flex; gap: 0">
          @for (i of cards; track i) { <li style="flex: 0 0 300px; height: 50px">Carte {{ i }}</li> }
        </ul>
      </app-carousel>
    </div>`,
})
class Host {
  cards = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];
}

describe('Carousel', () => {
  let fixture: ComponentFixture<Host>;
  let element: HTMLElement;

  beforeEach(async () => {
    fixture = TestBed.createComponent(Host);
    element = fixture.nativeElement;
    document.body.appendChild(element);   // dans la page : les tailles (scrollWidth…) sont calculées
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  });

  afterEach(() => element.remove());

  const track = () => element.querySelector<HTMLElement>('.carousel__track')!;
  const prev = () => element.querySelector<HTMLButtonElement>('.carousel__button--prev')!;
  const next = () => element.querySelector<HTMLButtonElement>('.carousel__button--next')!;

  it('est une zone nommée et atteignable au clavier (RGAA)', () => {
    expect(track().getAttribute('role')).toBe('region');
    expect(track().getAttribute('aria-label')).toBe('Événements (défilement horizontal)');
    expect(track().getAttribute('tabindex')).toBe('0');
  });

  it('au début : "précédent" désactivé, "suivant" actif, liés à la zone (aria-controls)', () => {
    expect(prev().disabled).toBeTrue();
    expect(next().disabled).toBeFalse();
    expect(next().getAttribute('aria-controls')).toBe('test-track');
  });

  it('les boutons ont un nom lisible par un lecteur d’écran', () => {
    expect(next().textContent).toContain('Événements suivants');
    expect(prev().textContent).toContain('Événements précédents');
  });
});
