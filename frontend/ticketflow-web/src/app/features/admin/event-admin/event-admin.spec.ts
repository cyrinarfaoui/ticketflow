import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting, TestRequest } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { API_BASE_URL } from '../../../core/config/api.config';
import { EventResponse } from '../../../core/services/event.service';
import { Page } from '../../../core/models/reservation';
import { DialogService } from '../../../shared/dialog/dialog.service';
import { EVENTS_PAGE_SIZE, EventAdmin } from './event-admin';

registerLocaleData(localeFr);

const JAZZ: EventResponse = {
  id: 'e1', title: 'Concert de jazz', description: 'Soirée jazz', location: 'Tunis', category: 'CONCERT',
  date: '2030-12-15T20:00:00', totalSeats: 100, availableSeats: 36, status: 'OPEN', priceCents: 5000, currency: 'TND',
};
const FESTIVAL: EventResponse = { ...JAZZ, id: 'e2', title: 'Festival', status: 'CANCELLED' };

function page(content: EventResponse[], totalPages = 1, totalElements = content.length): Page<EventResponse> {
  return { content, page: 0, size: 10, totalElements, totalPages } as Page<EventResponse>;
}

describe('EventAdmin (back-office)', () => {
  let fixture: ComponentFixture<EventAdmin>;
  let element: HTMLElement;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [EventAdmin],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: LOCALE_ID, useValue: 'fr' }],
    });
    httpMock = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(EventAdmin);
    element = fixture.nativeElement;
  });

  afterEach(() => httpMock.verify());

  const listRequest = (): TestRequest => httpMock.expectOne((r) => r.url === `${API_BASE_URL}/events/admin`);

  async function load(events = page([JAZZ, FESTIVAL])): Promise<void> {
    fixture.detectChanges();
    TestBed.tick();
    listRequest().flush(events);
    await fixture.whenStable();
    fixture.detectChanges();
  }

  function selectFirst(): void {
    element.querySelector<HTMLButtonElement>('.list-item')!.click();
    fixture.detectChanges();
  }

  async function answerDialog(confirmed: boolean): Promise<void> {
    TestBed.inject(DialogService).close(confirmed);
    await fixture.whenStable();
  }

  it('demande au serveur UNE page triée par date (petite page : pas de défilement)', () => {
    fixture.detectChanges();
    TestBed.tick();

    const request = listRequest();
    expect(request.request.params.get('page')).toBe('0');
    expect(request.request.params.get('size')).toBe(String(EVENTS_PAGE_SIZE));
    expect(request.request.params.get('sort')).toBe('date,asc');
    request.flush(page([]));
  });

  it('affiche le nombre total et la pagination', async () => {
    await load(page([JAZZ], 3, 25));

    expect(element.querySelector('.count')?.textContent).toContain('25 événement(s)');
    expect(element.querySelector('.pagination')?.textContent).toContain('Page 1 sur 3');
  });

  it('filtrer par statut redemande la première page au serveur', async () => {
    await load();

    const select = element.querySelector<HTMLSelectElement>('.filters select')!;
    select.value = 'CANCELLED';
    select.dispatchEvent(new Event('change'));
    TestBed.tick();

    const request = listRequest();
    expect(request.request.params.get('status')).toBe('CANCELLED');
    expect(request.request.params.get('page')).toBe('0');
    request.flush(page([FESTIVAL]));
  });

  it('un clic sur un événement remplit le formulaire (prix en TND)', async () => {
    await load();
    selectFirst();

    expect(element.querySelector('.list-item')?.classList).toContain('is-selected');
    expect(element.querySelector<HTMLInputElement>('input[formControlName="title"]')!.value).toBe('Concert de jazz');
    expect(element.querySelector<HTMLInputElement>('input[formControlName="price"]')!.value).toBe('50');
  });

  it('enregistre avec PUT, le prix converti en centimes', async () => {
    await load();
    selectFirst();

    const price = element.querySelector<HTMLInputElement>('input[formControlName="price"]')!;
    price.value = '60';
    price.dispatchEvent(new Event('input'));
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();

    const put = httpMock.expectOne(`${API_BASE_URL}/events/e1`);
    expect(put.request.method).toBe('PUT');
    expect(put.request.body.priceCents).toBe(6000);
    expect(put.request.body.date).toBe('2030-12-15T20:00:00');   // heure locale, sans décalage UTC
    put.flush({ ...JAZZ, priceCents: 6000 });
    TestBed.tick();
    listRequest().flush(page([{ ...JAZZ, priceCents: 6000 }, FESTIVAL]));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('.success')?.textContent).toContain('« Concert de jazz » enregistré.');
    expect(element.querySelector('.empty-state')).not.toBeNull();   // le formulaire s'est fermé
  });

  it('demande confirmation avant de supprimer, puis envoie DELETE', async () => {
    await load();
    selectFirst();

    [...element.querySelectorAll<HTMLButtonElement>('.actions button')].find((b) => b.textContent?.includes('Supprimer'))!.click();
    await answerDialog(true);

    const del = httpMock.expectOne(`${API_BASE_URL}/events/e1`);
    expect(del.request.method).toBe('DELETE');
    del.flush(null);
    TestBed.tick();
    listRequest().flush(page([FESTIVAL]));
  });

  it("n'envoie rien si l'admin clique « Retour »", async () => {
    await load();
    selectFirst();

    [...element.querySelectorAll<HTMLButtonElement>('.actions button')].find((b) => b.textContent?.includes('Supprimer'))!.click();
    await answerDialog(false);

    httpMock.expectNone(`${API_BASE_URL}/events/e1`);
  });

  it("annonce le refus du serveur dans une fenêtre d'erreur (409)", async () => {
    await load();
    selectFirst();

    [...element.querySelectorAll<HTMLButtonElement>('.actions button')].find((b) => b.textContent?.includes('Supprimer'))!.click();
    await answerDialog(true);
    httpMock.expectOne(`${API_BASE_URL}/events/e1`).flush(
      { error: 'Cet événement a des réservations : annulez-le plutôt que de le supprimer' },
      { status: 409, statusText: 'Conflict' },
    );

    const dialog = TestBed.inject(DialogService).current();
    expect(dialog?.tone).toBe('error');
    expect(dialog?.message).toContain('annulez-le');
  });

  // ---------- Création dans le même panneau ----------

  function clickNewEvent(): void {
    [...element.querySelectorAll<HTMLButtonElement>('.event-admin__header button')]
      .find((b) => b.textContent?.includes('Nouvel événement'))!.click();
    fixture.detectChanges();
  }

  function type(selector: string, value: string): void {
    const field = element.querySelector<HTMLInputElement | HTMLSelectElement>(selector)!;
    field.value = value;
    field.dispatchEvent(new Event(field.tagName === 'SELECT' ? 'change' : 'input'));
  }

  it('« Nouvel événement » remplace le panneau de droite par un formulaire de création vide', async () => {
    await load();
    selectFirst();

    clickNewEvent();

    expect(element.querySelector('.detail h2')?.textContent).toContain('Nouvel événement');
    expect(element.querySelector<HTMLInputElement>('input[formControlName="title"]')!.value).toBe('');
    expect(element.querySelector('.actions')?.textContent).toContain("Créer l'événement");
    expect(element.querySelector('.actions')?.textContent).not.toContain('Supprimer');
  });

  it('crée avec POST, puis ferme le formulaire en gardant le message de succès', async () => {
    await load();
    clickNewEvent();

    type('input[formControlName="title"]', 'Festival de Carthage');
    type('input[formControlName="location"]', 'Carthage');
    type('select[formControlName="category"]', 'FESTIVAL');
    type('input[formControlName="date"]', '2031-07-14T21:00');
    element.querySelector<HTMLButtonElement>('button[type="submit"]')!.click();

    const post = httpMock.expectOne(`${API_BASE_URL}/events`);
    expect(post.request.method).toBe('POST');
    expect(post.request.body).toEqual(jasmine.objectContaining({
      title: 'Festival de Carthage', category: 'FESTIVAL', date: '2031-07-14T21:00:00', totalSeats: 100, priceCents: 5000,
    }));
    const created: EventResponse = { ...JAZZ, id: 'e9', title: 'Festival de Carthage', category: 'FESTIVAL', date: '2031-07-14T21:00:00' };
    post.flush(created);
    TestBed.tick();
    listRequest().flush(page([JAZZ, FESTIVAL, created]));
    await fixture.whenStable();
    fixture.detectChanges();

    expect(element.querySelector('.success')?.textContent).toContain('« Festival de Carthage » créé.');
    expect(element.querySelector('.empty-state')).not.toBeNull();   // le formulaire s'est fermé
  });

  it('« Retour » quitte la création sans rien envoyer', async () => {
    await load();
    clickNewEvent();

    [...element.querySelectorAll<HTMLButtonElement>('.actions button')].find((b) => b.textContent?.includes('Retour'))!.click();
    fixture.detectChanges();

    expect(element.querySelector('.empty-state')).not.toBeNull();
    httpMock.expectNone(`${API_BASE_URL}/events`);
  });

  it('un événement passé est en lecture seule, avec le badge « Passé »', async () => {
    await load(page([{ ...JAZZ, date: '2020-01-01T20:00:00' }]));
    selectFirst();

    expect(element.querySelector('.list-item .badge')?.textContent).toContain('Passé');
    expect(element.querySelector<HTMLInputElement>('input[formControlName="title"]')!.disabled).toBeTrue();
    expect(element.querySelector('.detail')?.textContent).toContain('Événement passé');
    const labels = [...element.querySelectorAll('.actions button')].map((b) => b.textContent ?? '');
    expect(labels.some((label) => label.includes("Annuler l'événement"))).toBeFalse();
  });

  it('un événement annulé est en lecture seule', async () => {
    await load(page([FESTIVAL]));
    selectFirst();

    expect(element.querySelector<HTMLInputElement>('input[formControlName="title"]')!.disabled).toBeTrue();
    expect(element.querySelector<HTMLButtonElement>('button[type="submit"]')!.disabled).toBeTrue();
  });
});
