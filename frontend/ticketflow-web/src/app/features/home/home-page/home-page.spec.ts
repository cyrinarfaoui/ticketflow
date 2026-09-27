import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { HomePage } from './home-page';
import { AuthService } from '../../../core/services/auth.service';

describe('HomePage', () => {
  let fixture: ComponentFixture<HomePage>;
  let element: HTMLElement;

  beforeEach(async () => {
    const authService = jasmine.createSpyObj<AuthService>('AuthService', ['isLoggedIn', 'hasRole']);

    await TestBed.configureTestingModule({
      imports: [HomePage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(HomePage);
    element = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('a une seule zone de contenu principal', () => {
    expect(element.querySelectorAll('main').length).toBe(1);
  });

  it('contient la section "Tous les événements" avec la liste', () => {
    const section = element.querySelector('section#evenements');

    expect(section?.querySelector('h2')?.textContent?.trim()).toBe('Tous les événements');
    expect(section?.querySelector('app-event-list')).not.toBeNull();
  });

    it('affiche le hero en haut de la page', () => {
    const main = element.querySelector('main');

    expect(main?.firstElementChild?.tagName).toBe('APP-HERO');
  });

    it('n\'affiche la liste des événements qu\'une seule fois', () => {
    expect(element.querySelectorAll('app-event-list').length).toBe(1);
    expect(element.querySelectorAll('#evenements').length).toBe(1);
  });
});