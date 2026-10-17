import { afterEveryRender, Component, ElementRef, input, signal, viewChild } from '@angular/core';

/**
 * Rangée de cartes qui défile HORIZONTALEMENT (glisser au doigt, à la souris, au pavé tactile, ou flèches du clavier).
 * Les boutons Précédent / Suivant sont indispensables pour l'accessibilité :
 * tout le monde ne peut pas "glisser" (clavier seul, lecteur d'écran, troubles moteurs).
 *
 * Utilisation : <app-carousel label="Événements"><ul class="carousel-list">…</ul></app-carousel>
 */
@Component({
  selector: 'app-carousel',
  templateUrl: './carousel.html',
  styleUrl: './carousel.scss',
  // Fenêtre redimensionnée : le nombre de cartes visibles change, on recalcule les boutons
  host: { '(window:resize)': 'updateButtons()' },
})
export class Carousel {
  readonly label = input.required<string>();     // nom de la zone, lu par les lecteurs d'écran
  readonly trackId = input('carousel-track');

  private readonly track = viewChild.required<ElementRef<HTMLElement>>('track');
  protected readonly atStart = signal(true);
  protected readonly atEnd = signal(true);

  constructor() {
    // Après chaque affichage (filtres changés, cartes ajoutées…), on met à jour l'état des boutons
    afterEveryRender(() => this.updateButtons());
  }

  /** direction = -1 (précédent) ou 1 (suivant) : on avance d'un écran de cartes. */
  scroll(direction: number): void {
    const element = this.track().nativeElement;
    // Réglage "réduire les animations" : défilement instantané au lieu d'une animation
    const reduceMotion = window.matchMedia?.('(prefers-reduced-motion: reduce)').matches;
    element.scrollBy({ left: direction * element.clientWidth, behavior: reduceMotion ? 'auto' : 'smooth' });
  }

  protected updateButtons(): void {
    const element = this.track().nativeElement;
    this.atStart.set(element.scrollLeft <= 1);
    this.atEnd.set(element.scrollLeft + element.clientWidth >= element.scrollWidth - 1);
  }
}
