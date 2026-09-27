import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Hero } from './hero';
import { AuthService } from '../../../core/services/auth.service';

describe('Hero', () => {
  let fixture: ComponentFixture<Hero>;
  let element: HTMLElement;
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', [
      'isLoggedIn', 'getUsername', 'login', 'register',
    ]);

    await TestBed.configureTestingModule({
      imports: [Hero],
      providers: [{ provide: AuthService, useValue: authService }],
    }).compileComponents();
  });

  // Prépare l'état de connexion AVANT de créer le composant
  function render(loggedIn: boolean, username?: string): void {
    authService.isLoggedIn.and.returnValue(loggedIn);
    authService.getUsername.and.returnValue(username);
    fixture = TestBed.createComponent(Hero);
    element = fixture.nativeElement;
    fixture.detectChanges();
  }

  // Trouve un bouton ou un lien par son texte visible, comme un utilisateur
  function button(label: string): HTMLButtonElement | undefined {
    return [...element.querySelectorAll('button')].find((b) => b.textContent?.trim() === label);
  }

  function link(label: string): HTMLAnchorElement | undefined {
    return [...element.querySelectorAll('a')].find((a) => a.textContent?.trim() === label);
  }

  it('affiche un unique titre principal (h1)', () => {
    render(false);

    const titles = element.querySelectorAll('h1');
    expect(titles.length).toBe(1);
    expect(titles[0].textContent?.trim()).toBe('Trouvez votre prochain événement');
  });

  describe('visiteur non connecté', () => {

    beforeEach(() => render(false));

    it('appelle register() au clic sur "Créer un compte"', () => {
      button('Créer un compte')!.click();

      expect(authService.register).toHaveBeenCalledTimes(1);
    });

    it('appelle login() au clic sur "Se connecter"', () => {
      button('Se connecter')!.click();

      expect(authService.login).toHaveBeenCalledTimes(1);
    });

    it('propose de parcourir les événements sans compte', () => {
      expect(link('Parcourir les événements sans compte')?.getAttribute('href')).toBe('#evenements');
    });
  });

  describe('utilisateur connecté', () => {

    beforeEach(() => render(true, 'cyrine'));

    it('le salue par son nom et l\'envoie vers les événements', () => {
      expect(element.querySelector('.hero__greeting')?.textContent?.trim()).toBe('Bonjour cyrine');
      expect(link('Voir les événements')?.getAttribute('href')).toBe('#evenements');
    });

    it('ne lui propose plus de créer un compte ni de se connecter', () => {
      expect(button('Créer un compte')).toBeUndefined();
      expect(button('Se connecter')).toBeUndefined();
    });
  });
});