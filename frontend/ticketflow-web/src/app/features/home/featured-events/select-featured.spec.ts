import { selectFeatured } from './select-featured';
import { EventResponse } from '../../../core/services/event.service';

// L'heure "actuelle" figée pour tous les tests : le 27 septembre 2026 à midi
const NOW = new Date('2026-09-27T12:00:00');

// Fabrique d'événement : seuls l'id, la date et les places comptent ici
function event(id: string, date: string, availableSeats = 10): EventResponse {
  return {
    id,
    title: `Événement ${id}`,
    description: '',
    location: 'Tunis',
    category: 'CONCERT',
    date,
    totalSeats: 100,
    availableSeats,
  };
}

// Ne garde que les ids : les résultats sont plus faciles à lire et à comparer
function ids(events: EventResponse[]): string[] {
  return events.map((e) => e.id);
}

describe('selectFeatured', () => {

  it('trie les événements du plus proche au plus lointain', () => {
    const events = [
      event('dec', '2026-12-15T20:00:00'),
      event('oct', '2026-10-02T19:00:00'),
      event('nov', '2026-11-08T21:00:00'),
    ];

    expect(ids(selectFeatured(events, NOW))).toEqual(['oct', 'nov', 'dec']);
  });

  it('exclut les événements déjà passés', () => {
    const events = [
      event('hier', '2026-09-26T20:00:00'),
      event('demain', '2026-09-28T20:00:00'),
    ];

    expect(ids(selectFeatured(events, NOW))).toEqual(['demain']);
  });

  it('exclut un événement qui commence exactement maintenant', () => {
    const events = [event('maintenant', '2026-09-27T12:00:00')];

    expect(selectFeatured(events, NOW)).toEqual([]);
  });

  it('exclut les événements complets', () => {
    const events = [
      event('complet', '2026-10-01T20:00:00', 0),
      event('dispo', '2026-10-02T20:00:00', 5),
    ];

    expect(ids(selectFeatured(events, NOW))).toEqual(['dispo']);
  });

  it('garde les 3 premiers par défaut, ou le nombre demandé', () => {
    const events = ['01', '02', '03', '04', '05'].map((day) =>
      event(day, `2026-10-${day}T20:00:00`),
    );

    expect(ids(selectFeatured(events, NOW))).toEqual(['01', '02', '03']);
    expect(ids(selectFeatured(events, NOW, 2))).toEqual(['01', '02']);
  });

  it('renvoie une liste vide quand il n\'y a aucun événement', () => {
    expect(selectFeatured([], NOW)).toEqual([]);
  });

  it('ne modifie pas le tableau reçu', () => {
    const events = [
      event('dec', '2026-12-15T20:00:00'),
      event('oct', '2026-10-02T19:00:00'),
    ];

    selectFeatured(events, NOW);

    expect(ids(events)).toEqual(['dec', 'oct']);
  });
});