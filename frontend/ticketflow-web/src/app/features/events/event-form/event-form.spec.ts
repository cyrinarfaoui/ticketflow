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
      price: 19.99,
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
    expect(req.request.body.priceCents).toBe(1999);   // dinars -> centimes, arrondi
    req.flush({});
  });

  it('refuse une date passée et explique pourquoi', () => {
    fillValidForm();
    component.form.controls.date.setValue('2020-01-01T20:00');

    component.onSubmit();
    fixture.detectChanges();

    httpMock.expectNone(`${API_BASE_URL}/events`);
    expect(element.textContent).toContain('La date doit être dans le futur.');
  });

  it('refuse plus de 100 000 places', () => {
    fillValidForm();
    component.form.controls.totalSeats.setValue(100_001);

    component.onSubmit();
    fixture.detectChanges();

    httpMock.expectNone(`${API_BASE_URL}/events`);
    expect(element.textContent).toContain('100 000 places maximum.');
  });

    it('relie le message d\'erreur au champ pour les lecteurs d\'écran (RGAA)', () => {
    fillValidForm();
    component.form.controls.date.setValue('2020-01-01T20:00');

    component.onSubmit();
    fixture.detectChanges();

    const input = element.querySelector('#date')!;
    expect(input.getAttribute('aria-invalid')).toBe('true');
    expect(input.getAttribute('aria-describedby')).toBe('date-error');
    expect(element.querySelector('#date-error')?.textContent).toContain('La date doit être dans le futur.');
  });

  it('ne marque pas en erreur un champ valide', () => {
    fillValidForm();
    component.form.markAllAsTouched();
    fixture.detectChanges();

    expect(element.querySelector('#title')!.hasAttribute('aria-invalid')).toBeFalse();
  });

    it('désactive le bouton pendant l\'envoi et n\'envoie qu\'une seule requête', () => {
    fillValidForm();

    component.onSubmit();
    component.onSubmit();          // double clic
    fixture.detectChanges();

    const button = element.querySelector<HTMLButtonElement>('button[type="submit"]')!;
    expect(button.disabled).toBeTrue();
    expect(button.textContent).toContain('Création');
    httpMock.expectOne(`${API_BASE_URL}/events`).flush({});   // expectOne : il y en a UNE, pas deux
  });

  it('affiche le message du serveur si la création est refusée, puis réactive le bouton', () => {
    fillValidForm();
    component.onSubmit();

    httpMock
      .expectOne(`${API_BASE_URL}/events`)
      .flush({ error: 'Le nombre total de places doit être supérieur à 0' }, { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Le nombre total de places doit être supérieur à 0');
    expect(element.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBeFalse();
  });
  
});
