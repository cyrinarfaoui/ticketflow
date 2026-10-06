import { FormControl } from '@angular/forms';
import { futureDate } from './future-date.validator';


describe('futureDate (validateur)', () => {
  // "Maintenant" est figé : le test donne le même résultat quel que soit le jour où on le lance
  const now = new Date('2026-10-06T12:00:00');
  const validator = futureDate(() => now);

  it('accepte une date dans le futur', () => {
    expect(validator(new FormControl('2026-10-07T20:00'))).toBeNull();
  });

  it('refuse une date passée', () => {
    expect(validator(new FormControl('2026-10-05T20:00'))).toEqual({ pastDate: true });
  });

  it('refuse l\'instant présent : un événement doit être à venir', () => {
    expect(validator(new FormControl('2026-10-06T12:00'))).toEqual({ pastDate: true });
  });

  it('ne dit rien sur un champ vide : c\'est le rôle de Validators.required', () => {
    expect(validator(new FormControl(''))).toBeNull();
  });
});