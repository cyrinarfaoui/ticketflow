import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Refuse une date qui n'est pas strictement dans le futur.
 * `now` est passé en paramètre pour rendre le validateur testable (comme le Clock côté Java).
 */
export function futureDate(now: () => Date = () => new Date()): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    if (!control.value) {
      return null; // champ vide : c'est le rôle de Validators.required
    }
    const date = new Date(control.value);
    return date.getTime() > now().getTime() ? null : { pastDate: true };
  };
}