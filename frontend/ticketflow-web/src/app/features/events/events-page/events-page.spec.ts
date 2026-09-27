import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { EventsPage } from './events-page';
import { AuthService } from '../../../core/services/auth.service';

describe('EventsPage', () => {
  let element: HTMLElement;

  beforeEach(async () => {
    const authService = jasmine.createSpyObj<AuthService>('AuthService', ['hasRole']);

    await TestBed.configureTestingModule({
      imports: [EventsPage],
      providers: [
        provideRouter([]),
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authService },
      ],
    }).compileComponents();

    const fixture: ComponentFixture<EventsPage> = TestBed.createComponent(EventsPage);
    element = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('a un unique titre principal "Tous les événements"', () => {
    const titles = element.querySelectorAll('h1');

    expect(titles.length).toBe(1);
    expect(titles[0].textContent?.trim()).toBe('Tous les événements');
  });

  it('affiche la liste des événements avec la recherche dans le contenu principal', () => {
    expect(element.querySelectorAll('main').length).toBe(1);
    expect(element.querySelector('main app-event-list')).not.toBeNull();
  });
});