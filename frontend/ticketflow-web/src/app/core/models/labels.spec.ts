import { categoryLabel, EventCategory } from './event-category';
import { reservationStatusLabel, ReservationStatus } from './reservation';

describe('Libellés affichés', () => {

  it('traduit chaque catégorie en français', () => {
    expect(categoryLabel('THEATRE')).toBe('Théâtre');
    expect(categoryLabel('CONFERENCE')).toBe('Conférence');
  });

  it('traduit chaque statut de réservation en français', () => {
    expect(reservationStatusLabel('PENDING')).toBe('En attente');
    expect(reservationStatusLabel('EXPIRED')).toBe('Expirée');
  });

  // Si le backend ajoute une valeur que le front ne connaît pas encore, on affiche la valeur brute
  // plutôt que "undefined" ou une erreur
  it('affiche la valeur brute pour une catégorie inconnue', () => {
    expect(categoryLabel('OPERA' as EventCategory)).toBe('OPERA');
  });

  it('affiche la valeur brute pour un statut inconnu', () => {
    expect(reservationStatusLabel('REFUNDED' as ReservationStatus)).toBe('REFUNDED');
  });
});
