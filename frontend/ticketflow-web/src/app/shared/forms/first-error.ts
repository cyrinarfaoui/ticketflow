import { AbstractControl } from '@angular/forms';

// Clé d'erreur Angular (required, minlength, pastDate…) → message affiché à l'utilisateur
export type ErrorMessages = Record<string, string>;

/**
 * Le message à afficher pour un champ, ou null.
 * - Rien tant que l'utilisateur n'a pas touché le champ (pas d'erreur rouge sur un formulaire vierge)
 * - Une seule erreur à la fois : la première du dictionnaire, dans l'ordre où il est écrit
 */
export function firstError(control: AbstractControl, messages: ErrorMessages): string | null {
  if (!control.touched || !control.errors) {
    return null;
  }
  const key = Object.keys(messages).find((errorKey) => control.hasError(errorKey));
  return key ? messages[key] : null;
}