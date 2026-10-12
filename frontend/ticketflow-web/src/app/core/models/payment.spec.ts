import { declineLabel, formatPrice, TEST_CARDS } from './payment';

describe('payment', () => {

  it('formate un montant en centimes', () => {
    expect(formatPrice(5000, 'TND')).toBe('50,00 TND');
    expect(formatPrice(12345, 'TND')).toBe('123,45 TND');
  });

  it('traduit les codes de refus de Stripe', () => {
    expect(declineLabel('insufficient_funds')).toBe('Paiement refusé : fonds insuffisants');
    expect(declineLabel('expired_card')).toBe('Paiement refusé : carte expirée');
  });

  it('affiche le code brut pour un motif inconnu', () => {
    expect(declineLabel('processing_error')).toBe('Paiement refusé : processing_error');
  });

  it('ne propose que des jetons, jamais de numéro de carte', () => {
    expect(TEST_CARDS.every((card) => /^pm_[A-Za-z0-9_]+$/.test(card.token))).toBeTrue();
  });
});
