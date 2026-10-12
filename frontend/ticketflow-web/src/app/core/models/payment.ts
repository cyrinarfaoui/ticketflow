// Paiement "façon Stripe" : le navigateur n'envoie JAMAIS de numéro de carte au backend,
// seulement un JETON (pm_…) que le prestataire a créé. Ici on simule le choix d'une carte de test.

export interface TestCard {
  token: string;
  label: string;
}

// Jetons de test reconnus par FakeStripeProvider (payment-service)
export const TEST_CARDS: readonly TestCard[] = [
  { token: 'pm_card_visa', label: 'Visa (acceptée)' },
  { token: 'pm_card_mastercard', label: 'Mastercard (acceptée)' },
  { token: 'pm_card_chargeDeclined', label: 'Carte refusée' },
  { token: 'pm_card_insufficientFunds', label: 'Fonds insuffisants' },
  { token: 'pm_card_expired', label: 'Carte expirée' },
];

// Ce que la carte d'événement envoie au parent quand on clique sur "Réserver"
export interface ReserveRequest {
  seats: number;
  paymentMethod: string;
}

// Codes de refus (mêmes codes que Stripe) -> message lisible
const DECLINE_LABELS: Record<string, string> = {
  card_declined: 'carte refusée',
  insufficient_funds: 'fonds insuffisants',
  expired_card: 'carte expirée',
  invalid_payment_method: 'moyen de paiement invalide',
  invalid_amount: 'montant invalide',
  amount_too_large: 'montant trop élevé',
};

export function declineLabel(code: string): string {
  return `Paiement refusé : ${DECLINE_LABELS[code] ?? code}`;
}

// Montant stocké en centimes (entier, jamais de float) -> "50,00 TND"
// (simplification : le dinar réel a 3 décimales, les millimes ; on garde la convention "centimes" du backend)
export function formatPrice(cents: number, currency: string): string {
  return `${(cents / 100).toFixed(2).replace(".", ",")} ${currency}`;
}
