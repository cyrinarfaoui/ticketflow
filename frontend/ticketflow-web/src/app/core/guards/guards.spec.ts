import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, provideRouter, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { adminGuard } from './admin.guard';
import { authGuard } from './auth.guard';
import { homeGuard } from './home.guard';

/**
 * Tests unitaires des guards fonctionnels.
 * Un guard utilise inject() : il doit donc être appelé dans un "contexte d'injection",
 * d'où TestBed.runInInjectionContext().
 */
describe('Guards', () => {
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', ['isLoggedIn', 'hasRole']);
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: authService }],
    });
  });

  // Les guards n'utilisent pas la route ni l'état : des objets vides suffisent
  const route = {} as ActivatedRouteSnapshot;
  const state = {} as RouterStateSnapshot;

  function homeUrl(): UrlTree {
    return TestBed.inject(Router).createUrlTree(['/']);
  }

  describe('authGuard', () => {

    it('laisse passer un utilisateur connecté', () => {
      authService.isLoggedIn.and.returnValue(true);

      const result = TestBed.runInInjectionContext(() => authGuard(route, state));

      expect(result).toBeTrue();
    });

    it('renvoie un visiteur vers l\'accueil', () => {
      authService.isLoggedIn.and.returnValue(false);

      const result = TestBed.runInInjectionContext(() => authGuard(route, state));

      expect(result).toEqual(homeUrl());
    });
  });

  describe('adminGuard', () => {

    it('laisse passer un administrateur connecté', () => {
      authService.isLoggedIn.and.returnValue(true);
      authService.hasRole.and.returnValue(true);

      const result = TestBed.runInInjectionContext(() => adminGuard(route, state));

      expect(result).toBeTrue();
      expect(authService.hasRole).toHaveBeenCalledWith('ADMIN');
    });

    it('refuse un utilisateur connecté sans le rôle ADMIN', () => {
      authService.isLoggedIn.and.returnValue(true);
      authService.hasRole.and.returnValue(false);

      const result = TestBed.runInInjectionContext(() => adminGuard(route, state));

      expect(result).toEqual(homeUrl());
    });

    it('refuse un visiteur non connecté', () => {
      authService.isLoggedIn.and.returnValue(false);
      authService.hasRole.and.returnValue(true);

      const result = TestBed.runInInjectionContext(() => adminGuard(route, state));

      expect(result).toEqual(homeUrl());
    });
  });

  describe('homeGuard', () => {

    it("envoie l'administrateur sur son tableau de bord", () => {
      authService.hasRole.and.returnValue(true);

      const result = TestBed.runInInjectionContext(() => homeGuard(route, state));

      expect(result).toEqual(TestBed.inject(Router).createUrlTree(['/admin']));
    });

    it("laisse les clients et les visiteurs sur l'accueil", () => {
      authService.hasRole.and.returnValue(false);

      const result = TestBed.runInInjectionContext(() => homeGuard(route, state));

      expect(result).toBeTrue();
    });
  });
});
