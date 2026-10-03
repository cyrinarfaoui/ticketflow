import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { EventForm } from './event-form';
import { API_BASE_URL } from '../../../core/config/api.config';

describe('EventForm', () => {
  let fixture: ComponentFixture<EventForm>;
  let component: EventForm;
  let element: HTMLElement;
  let httpMock: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [EventForm],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(EventForm);
    component = fixture.componentInstance;
    element = fixture.nativeElement;
    fixture.detectChanges();
  });

  afterEach(() => httpMock.verify());

  function fillValidForm(): void {
    component.form.setValue({
      title: 'Concert de jazz',
      description: 'Soirée jazz',
      location: 'Tunis',
      category: 'CONCERT',
      date: '2030-12-15T20:00',
      totalSeats: 100,
    });
  }

  it('propose toutes les catégories dans la liste déroulante', () => {
    const options = [...element.querySelectorAll<HTMLOptionElement>('#category option')]
      .map((option) => option.textContent?.trim());

    expect(options).toEqual(['Choisir une catégorie', 'Concert', 'Théâtre', 'Conférence', 'Sport', 'Festival', 'Autre']);
  });

  it('refuse l\'envoi sans catégorie et affiche une erreur', () => {
    fillValidForm();
    component.form.controls.category.setValue('');

    component.onSubmit();
    fixture.detectChanges();

    httpMock.expectNone(`${API_BASE_URL}/events`);
    expect(element.textContent).toContain('La catégorie est obligatoire.');
  });

  it('envoie la catégorie choisie à l\'API', () => {
    fillValidForm();

    component.onSubmit();

    const req = httpMock.expectOne(`${API_BASE_URL}/events`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body.category).toBe('CONCERT');
    req.flush({});
  });
});
