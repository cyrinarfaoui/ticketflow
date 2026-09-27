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

  it('ne contient plus la liste complète des événements (elle est sur /evenements)', () => {
    expect(element.querySelector('app-event-list')).toBeNull();
  });

  it('affiche le hero en haut de la page', () => {
    const main = element.querySelector('main');

    expect(main?.firstElementChild?.tagName).toBe('APP-HERO');
  });

 
    it('affiche "À la une" juste après le hero', () => {
    const main = element.querySelector('main');

    expect(main?.children[1]?.tagName).toBe('APP-FEATURED-EVENTS');
  });
});