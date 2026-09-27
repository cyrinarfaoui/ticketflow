import { Component, computed } from '@angular/core';
import { httpResource } from '@angular/common/http';
import { EventCard } from '../../events/event-card/event-card';
import { EventResponse } from '../../../core/services/event.service';
import { API_BASE_URL } from '../../../core/config/api.config';
import { selectFeatured } from './select-featured';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-featured-events',
  imports: [EventCard,RouterLink],
  templateUrl: './featured-events.html',
  styleUrl: './featured-events.scss',
})
export class FeaturedEvents {

  // La requête HTTP, transformée en signal (valeur, chargement, erreur)
  private readonly events = httpResource<EventResponse[]>(
    () => `${API_BASE_URL}/events`,
    { defaultValue: [] },
  );

  protected readonly isLoading = this.events.isLoading;
  protected readonly hasError = computed(() => this.events.error() !== undefined);

  // La règle métier (étape 6) appliquée aux données chargées
  protected readonly featured = computed(() => selectFeatured(this.events.value(), new Date()));
}