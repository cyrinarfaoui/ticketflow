import { Component, signal, WritableSignal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { App } from './app';
import { AuthService } from './core/services/auth.service';
import { ReservationStore } from './core/stores/reservation.store';
import { LiveNotificationStore } from './core/stores/live-notification.store';

// Une page vide : pour tester le header, on n'a pas besoin des vraies pages
@Component({ template: '' })
class BlankPage {}

describe('App', () => {
  let authService: jasmine.SpyObj<AuthService>;
  // Faux store : le header ne fait que LIRE activeCount ; on contrôle sa valeur avec activeCount.set(...)
  let reservationStore: { activeCount: WritableSignal<number>; load: jasmine.Spy; clear: jasmine.Spy };
  // Faux store du flux en direct : pas de vraie connexion SSE pendant les tests
  let liveStore: { connect: jasmine.Spy; disconnect: jasmine.Spy; markAllRead: jasmine.Spy;
    notifications: WritableSignal<never[]>; unreadCount: WritableSignal<number>; status: WritableSignal<string> };

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', [
      'isLoggedIn', 'username', 'login', 'logout', 'register', 'hasRole',
    ]);
    liveStore = {
      connect: jasmine.createSpy('connect'),
      disconnect: jasmine.createSpy('disconnect'),
      markAllRead: jasmine.createSpy('markAllRead'),
      notifications: signal([]),
      unreadCount: signal(0),
      status: signal('open'),
    };
    reservationStore = {
      activeCount: signal(0),
      load: jasmine.createSpy('load'),
      clear: jasmine.createSpy('clear'),
    };

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([
          { path: '', component: BlankPage },
          { path: 'evenements', component: BlankPage },
        ]),
        { provide: AuthService, useValue: authService },
        { provide: ReservationStore, useValue: reservationStore },
        { provide: LiveNotificationStore, useValue: liveStore },
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
      authService.username.and.returnValue('cyrine');
      const element = await renderAt('/');

      expect(element.textContent).toContain('Connecté : cyrine');
    });
  });

  describe('navigation', () => {

     it('affiche la bannière de notification juste sous le header, sur toutes les pages', async () => {
      const element = await renderAt('/evenements');

      expect(element.querySelector('header + app-notification-banner')).not.toBeNull();
    });

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

    it('cache le lien "Mes réservations" à un visiteur non connecté', async () => {
      authService.isLoggedIn.and.returnValue(false);
      const element = await renderAt('/');

      expect(navLink(element, 'Mes réservations')).toBeUndefined();
    });

    it('affiche le lien "Mes réservations" à un utilisateur connecté', async () => {
      authService.isLoggedIn.and.returnValue(true);
      const element = await renderAt('/');

      expect(navLink(element, 'Mes réservations')?.getAttribute('href')).toBe('/mes-reservations');
    });

    it('cache le lien "Tableau de bord" à un utilisateur non admin', async () => {
      authService.hasRole.and.returnValue(false);
      const element = await renderAt('/');

      expect(navLink(element, 'Tableau de bord')).toBeUndefined();
    });

    it('affiche le lien "Tableau de bord" à un admin', async () => {
      authService.hasRole.and.callFake((role) => role === 'ADMIN');
      const element = await renderAt('/');

      expect(navLink(element, 'Tableau de bord')?.getAttribute('href')).toBe('/admin');
    });
  });

  describe('badge des réservations (store partagé)', () => {

    function badge(element: HTMLElement): HTMLElement | null {
      return element.querySelector('.nav-badge');
    }

    it('affiche le nombre de réservations en cours, lisible par un lecteur d\'écran', async () => {
      authService.isLoggedIn.and.returnValue(true);
      reservationStore.activeCount.set(2);

      const element = await renderAt('/');

      expect(badge(element)?.textContent?.trim()).toBe('2 en cours');
    });

    it('n\'affiche pas de badge quand il n\'y a aucune réservation en cours', async () => {
      authService.isLoggedIn.and.returnValue(true);

      const element = await renderAt('/');

      expect(badge(element)).toBeNull();
    });

    it('charge les réservations quand l\'utilisateur est connecté', async () => {
      authService.isLoggedIn.and.returnValue(true);

      await renderAt('/');

      expect(reservationStore.load).toHaveBeenCalled();
      expect(reservationStore.clear).not.toHaveBeenCalled();
    });

    it('vide le store quand personne n\'est connecté', async () => {
      authService.isLoggedIn.and.returnValue(false);

      await renderAt('/');

      expect(reservationStore.clear).toHaveBeenCalled();
      expect(reservationStore.load).not.toHaveBeenCalled();
    });
  });

  describe('activité en direct', () => {

    it("ouvre le flux SSE au démarrage, même sans être connecté (flux public)", async () => {
      authService.isLoggedIn.and.returnValue(false);
      const element = await renderAt('/');

      expect(liveStore.connect).toHaveBeenCalledTimes(1);
      expect(element.querySelector('app-live-notifications .bell')).not.toBeNull();
    });

    it("ferme le flux quand l'application est détruite", async () => {
      authService.isLoggedIn.and.returnValue(false);
      const fixture = TestBed.createComponent(App);

      fixture.destroy();

      expect(liveStore.disconnect).toHaveBeenCalled();
    });
  });
});
