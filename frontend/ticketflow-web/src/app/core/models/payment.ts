// Paiement "façon Stripe" : le numéro de carte est saisi dans le navigateur, transformé en JETON (pm_…),
// et SEUL le jeton part vers notre API. Le backend ne voit jamais de numéro de carte (périmètre PCI-DSS).

export interface TestCard {
  number: string;   // sans espaces
  label: string;    // résultat attendu, affiché dans l'aide "Mode test"
  token: string;    // jeton de test Stripe correspondant (reconnu par FakeStripeProvider)
}

// Cartes de test publiées par Stripe : en mode test, c'est la CARTE qui décide du résultat
export const TEST_CARDS: readonly TestCard[] = [
  { number: '4242424242424242', label: 'Visa — acceptée', token: 'pm_card_visa' },
  { number: '5555555555554444', label: 'Mastercard — acceptée', token: 'pm_card_mastercard' },
  { number: '4000000000000002', label: 'Carte refusée', token: 'pm_card_chargeDeclined' },
  { number: '4000000000009995', label: 'Fonds insuffisants', token: 'pm_card_insufficientFunds' },
  { number: '4000000000000069', label: 'Carte expirée', token: 'pm_card_expired' },
];

/** Garde uniquement les chiffres : "4242 4242-4242" → "42424242424242". */
export function digitsOnly(value: string): string {
  return value.replace(/\D/g, '');
}

/** Affichage par groupes de 4, comme sur une carte : "4242424242424242" → "4242 4242 4242 4242". */
export function formatCardNumber(value: string): string {
  return digitsOnly(value).slice(0, 16).replace(/(\d{4})(?=\d)/g, '$1 ');
}

/**
 * Algorithme de Luhn : la "clé de contrôle" de tous les numéros de carte.
 * Détecte une faute de frappe AVANT d'appeler le prestataire (c'est ce que font les vrais formulaires).
 */
export function isValidCardNumber(value: string): boolean {
  const digits = digitsOnly(value);
  if (digits.length < 13 || digits.length > 19) {
    return false;
  }
  let sum = 0;
  for (let i = 0; i < digits.length; i++) {
    let digit = Number(digits[digits.length - 1 - i]);
    if (i % 2 === 1) {          // un chiffre sur deux, en partant de la droite, est doublé
      digit *= 2;
      if (digit > 9) {
        digit -= 9;
      }
    }
    sum += digit;
  }
  return sum % 10 === 0;
}

/** "MM/AA" valide et pas encore dépassé (une carte expire à la FIN du mois indiqué). */
export function isValidExpiry(value: string, today: Date = new Date()): boolean {
  const match = /^(0[1-9]|1[0-2])\/(\d{2})$/.exec(value);
  if (!match) {
    return false;
  }
  const month = Number(match[1]);
  const year = 2000 + Number(match[2]);
  const firstDayAfterExpiry = new Date(year, month, 1);   // mois suivant (les mois JS commencent à 0)
  return firstDayAfterExpiry > today;
}

/**
 * Simule Stripe.js : transforme un numéro de carte en jeton.
 * Comme Stripe en mode test, une carte qui n'est pas une carte de test est refusée (null).
 */
export function tokenizeTestCard(cardNumber: string): string | null {
  const digits = digitsOnly(cardNumber);
  return TEST_CARDS.find((card) => card.number === digits)?.token ?? null;
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
  return `${(cents / 100).toFixed(2).replace('.', ',')} ${currency}`;
}
