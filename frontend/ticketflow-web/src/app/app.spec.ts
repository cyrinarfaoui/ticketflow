import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { AuthService } from './core/services/auth.service';

describe('App', () => {
  let authService: jasmine.SpyObj<AuthService>;

  beforeEach(async () => {
    authService = jasmine.createSpyObj<AuthService>('AuthService', [
      'isLoggedIn', 'getUsername', 'login', 'logout', 'register',
    ]);

    await TestBed.configureTestingModule({
      imports: [App],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();
  });

  it('appelle login() au clic sur "Se connecter" quand l\'utilisateur est déconnecté', () => {
    authService.isLoggedIn.and.returnValue(false);
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    const element: HTMLElement = fixture.nativeElement;
    const loginButton = [...element.querySelectorAll('button')]
      .find((button) => button.textContent?.includes('Se connecter'))!;
    loginButton.click();

    expect(authService.login).toHaveBeenCalledTimes(1);
  });

  it('affiche le nom de l\'utilisateur connecté', () => {
    authService.isLoggedIn.and.returnValue(true);
    authService.getUsername.and.returnValue('cyrine');
    const fixture = TestBed.createComponent(App);
    fixture.detectChanges();

    expect(fixture.nativeElement.textContent).toContain('Connecté : cyrine');
  });
});