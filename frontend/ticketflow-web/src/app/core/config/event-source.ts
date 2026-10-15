import { InjectionToken } from '@angular/core';

/**
 * Fabrique d'EventSource injectable : l'application utilise le vrai EventSource du navigateur,
 * les tests en fournissent un faux (pour simuler messages, coupures et reconnexions sans serveur).
 */
export const EVENT_SOURCE_FACTORY = new InjectionToken<(url: string) => EventSource>('EVENT_SOURCE_FACTORY', {
  providedIn: 'root',
  factory: () => (url: string) => new EventSource(url),
});
