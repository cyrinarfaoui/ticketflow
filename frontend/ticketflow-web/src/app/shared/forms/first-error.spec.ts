import { FormControl, Validators } from '@angular/forms';
import { firstError } from '../../shared/forms/first-error';

describe('firstError', () => {
  const messages = {
    required: 'Champ obligatoire.',
    minlength: 'Trop court.',
  };

  it('ne montre rien tant que l\'utilisateur n\'a pas touché le champ', () => {
    const control = new FormControl('', Validators.required);

    expect(firstError(control, messages)).toBeNull();
  });

  it('renvoie le message de l\'erreur présente une fois le champ touché', () => {
    const control = new FormControl('ab', Validators.minLength(3));
    control.markAsTouched();

    expect(firstError(control, messages)).toBe('Trop court.');
  });

  it('respecte l\'ordre de priorité du dictionnaire', () => {
    // vide : required ET minlength pourraient s'appliquer → on n'affiche que le premier
    const control = new FormControl('', [Validators.minLength(3), Validators.required]);
    control.markAsTouched();

    expect(firstError(control, messages)).toBe('Champ obligatoire.');
  });

  it('renvoie null quand le champ est valide', () => {
    const control = new FormControl('abc', Validators.minLength(3));
    control.markAsTouched();

    expect(firstError(control, messages)).toBeNull();
  });
});