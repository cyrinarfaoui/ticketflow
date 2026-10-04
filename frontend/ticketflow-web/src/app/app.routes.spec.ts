import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { AuthService } from './core/services/auth.service';
import { EventForm } from './features/events/event-form/event-form';
import { routes } from './app.routes';
import { HomePage } from './features/home/home-page/home-page';
import { EventsPage } from './features/events/events-page/events-page';
import { AdminDashboard } from './features/admin/admin-dashboard/admin-dashboard';
import { MyReservations } from './features/reservations/my-reservations/my-reservations';

describe('Routes de l\'application (sécurité)', () => {
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(() => {
       authService = jasmine.createSpyObj<AuthService>('AuthService', [
      'isLoggedIn', 'hasRole', 'getUsername', 'login', 'register',
    ]);

    TestBed.configureTestingModule({
      providers: [
        provideRouter(routes),              // les VRAIES routes de l'application
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService },
      ],
    });
  });

  it('redirige un visiteur non connecté vers l\'accueil', async () => {
    authService.isLoggedIn.and.returnValue(false);
    authService.hasRole.and.returnValue(false);

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/events/new');

    expect(TestBed.inject(Router).url).toBe('/');
  });

  it('redirige un utilisateur connecté mais non admin vers l\'accueil', async () => {
    authService.isLoggedIn.and.returnValue(true);
    authService.hasRole.and.returnValue(false);

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/events/new');

    expect(TestBed.inject(Router).url).toBe('/');
  });

  it('laisse un admin accéder au formulaire de création', async () => {
    authService.isLoggedIn.and.returnValue(true);
    authService.hasRole.and.returnValue(true);

    const harness = await RouterTestingHarness.create();
    const page = await harness.navigateByUrl('/events/new', EventForm);

    expect(TestBed.inject(Router).url).toBe('/events/new');
    expect(page).toBeInstanceOf(EventForm);
  });

  it('affiche la page d\'accueil à la racine du site', async () => {
    const harness = await RouterTestingHarness.create();

    const page = await harness.navigateByUrl('/', HomePage);

    expect(page).toBeInstanceOf(HomePage);
  });

  it('renvoie un visiteur non connecté vers l\'accueil s\'il ouvre "Mes réservations"', async () => {
    authService.isLoggedIn.and.returnValue(false);

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/mes-reservations');

    expect(TestBed.inject(Router).url).toBe('/');
  });

  it('ouvre "Mes réservations" pour un utilisateur connecté', async () => {
    authService.isLoggedIn.and.returnValue(true);

    const harness = await RouterTestingHarness.create();
    const page = await harness.navigateByUrl('/mes-reservations', MyReservations);

    expect(page).toBeInstanceOf(MyReservations);
  });

  it('refuse le tableau de bord à un utilisateur non admin', async () => {
    authService.isLoggedIn.and.returnValue(true);
    authService.hasRole.and.returnValue(false);

    const harness = await RouterTestingHarness.create();
    await harness.navigateByUrl('/admin');

    expect(TestBed.inject(Router).url).toBe('/');
  });

  it('charge le tableau de bord (lazy loading) pour un admin', async () => {
    authService.isLoggedIn.and.returnValue(true);
    authService.hasRole.and.returnValue(true);

    const harness = await RouterTestingHarness.create();
    const page = await harness.navigateByUrl('/admin', AdminDashboard);

    expect(page).toBeInstanceOf(AdminDashboard);
  });

    it('affiche le catalogue des événements sur /evenements', async () => {
    const harness = await RouterTestingHarness.create();

    const page = await harness.navigateByUrl('/evenements', EventsPage);

    expect(page).toBeInstanceOf(EventsPage);
  });
});