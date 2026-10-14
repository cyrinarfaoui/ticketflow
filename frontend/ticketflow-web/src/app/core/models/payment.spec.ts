import { declineLabel, formatCardNumber, formatPrice, isValidCardNumber, isValidExpiry, TEST_CARDS, tokenizeTestCard } from './payment';

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

  it('associe chaque carte de test à un jeton Stripe pm_…', () => {
    expect(TEST_CARDS.every((card) => /^pm_[A-Za-z0-9_]+$/.test(card.token))).toBeTrue();
  });

  it('affiche le numéro par groupes de 4 chiffres', () => {
    expect(formatCardNumber('4242424242424242')).toBe('4242 4242 4242 4242');
    expect(formatCardNumber('4242-42a')).toBe('4242 42');
  });

  it("valide la clé de Luhn (détecte une faute de frappe avant d'appeler la banque)", () => {
    expect(isValidCardNumber('4242 4242 4242 4242')).toBeTrue();
    expect(isValidCardNumber('4242 4242 4242 4241')).toBeFalse();   // un chiffre faux
    expect(isValidCardNumber('4242')).toBeFalse();                  // trop court
  });

  it("accepte une date d'expiration jusqu'à la fin du mois indiqué", () => {
    const today = new Date(2026, 9, 14);   // 14 octobre 2026
    expect(isValidExpiry('10/26', today)).toBeTrue();
    expect(isValidExpiry('09/26', today)).toBeFalse();
    expect(isValidExpiry('13/30', today)).toBeFalse();
    expect(isValidExpiry('1230', today)).toBeFalse();
  });

  it('transforme une carte de test en jeton, et refuse une carte réelle (mode test)', () => {
    expect(tokenizeTestCard('4000 0000 0000 9995')).toBe('pm_card_insufficientFunds');
    expect(tokenizeTestCard('4111 1111 1111 1111')).toBeNull();     // numéro valide, mais pas une carte de test
  });

  it('les cartes de test ont toutes un numéro valide', () => {
    expect(TEST_CARDS.every((card) => isValidCardNumber(card.number))).toBeTrue();
  });
});
