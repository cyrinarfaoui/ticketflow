import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { AuthService } from './core/services/auth.service';

// Une page vide : pour tester le header, on n'a pas besoin des vraies pages
@Component({ template: '' })
class BlankPage {}

describe('App', () => {
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', [
      'isLoggedIn', 'getUsername', 'login', 'logout', 'register',
    ]);

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([
          { path: '', component: BlankPage },
          { path: 'evenements', component: BlankPage },
        ]),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();
  });

  // Crée l'application, va sur l'URL demandée, puis met le HTML à jour
  async function renderAt(url: string): Promise<HTMLElement> {
    const fixture = TestBed.createComponent(App);
    await TestBed.inject(Router).navigateByUrl(url);
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture.nativeElement;
  }

  function navLink(element: HTMLElement, label: string): HTMLAnchorElement | undefined {
    return [...element.querySelectorAll<HTMLAnchorElement>('nav a')]
      .find((a) => a.textContent?.trim() === label);
  }

  describe('connexion', () => {

    it('appelle login() au clic sur "Se connecter" quand l\'utilisateur est déconnecté', async () => {
      authService.isLoggedIn.and.returnValue(false);
      const element = await renderAt('/');

      const loginButton = [...element.querySelectorAll('button')]
        .find((button) => button.textContent?.includes('Se connecter'))!;
      loginButton.click();

      expect(authService.login).toHaveBeenCalledTimes(1);
    });

    it('affiche le nom de l\'utilisateur connecté', async () => {
      authService.isLoggedIn.and.returnValue(true);
      authService.getUsername.and.returnValue('cyrine');
      const element = await renderAt('/');

      expect(element.textContent).toContain('Connecté : cyrine');
    });
  });

  describe('navigation', () => {

    it('le logo ramène à l\'accueil', async () => {
      const element = await renderAt('/evenements');

      expect(element.querySelector('.app-logo')?.getAttribute('href')).toBe('/');
    });

    it('propose les liens Accueil et Événements', async () => {
      const element = await renderAt('/');

      expect(navLink(element, 'Accueil')?.getAttribute('href')).toBe('/');
      expect(navLink(element, 'Événements')?.getAttribute('href')).toBe('/evenements');
    });

    it('indique la page actuelle sur /evenements', async () => {
      const element = await renderAt('/evenements');

      expect(navLink(element, 'Événements')?.getAttribute('aria-current')).toBe('page');
      expect(navLink(element, 'Accueil')?.hasAttribute('aria-current')).toBeFalse();
    });

    it('indique la page actuelle sur l\'accueil', async () => {
      const element = await renderAt('/');

      expect(navLink(element, 'Accueil')?.getAttribute('aria-current')).toBe('page');
      expect(navLink(element, 'Événements')?.hasAttribute('aria-current')).toBeFalse();
    });
  });
});