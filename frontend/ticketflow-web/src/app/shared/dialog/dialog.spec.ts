import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DialogHost } from './dialog-host';
import { DialogService } from './dialog.service';

describe('Fenêtres de dialogue', () => {
  let service: DialogService;
  let fixture: ComponentFixture<DialogHost>;
  let element: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [DialogHost] });
    service = TestBed.inject(DialogService);
    fixture = TestBed.createComponent(DialogHost);
    element = fixture.nativeElement;
    fixture.detectChanges();
  });

  const buttons = () => [...element.querySelectorAll<HTMLButtonElement>('.app-dialog__actions button')];

  it('affiche le titre, le message, "Retour" puis le bouton de confirmation', () => {
    service.confirm({ title: 'Supprimer l\'événement', message: 'Action irréversible.', confirmLabel: 'Supprimer', tone: 'danger' });
    fixture.detectChanges();

    expect(element.querySelector('dialog')!.open).toBeTrue();
    expect(element.querySelector('h2')?.textContent).toContain('Supprimer l\'événement');
    expect(element.querySelector('p')?.textContent).toContain('Action irréversible.');
    expect(buttons().map((b) => b.textContent?.trim())).toEqual(['Retour', 'Supprimer']);
    expect(element.querySelector('dialog')?.getAttribute('data-tone')).toBe('danger');
  });

  it('résout true quand on confirme, puis ferme la fenêtre', async () => {
    const answer = service.confirm({ title: 'Clôturer', message: '…', confirmLabel: 'Clôturer' });
    fixture.detectChanges();

    buttons()[1].click();
    fixture.detectChanges();

    expect(await answer).toBeTrue();
    expect(element.querySelector('dialog')!.open).toBeFalse();
  });

  it('résout false quand on clique "Retour"', async () => {
    const answer = service.confirm({ title: 'Clôturer', message: '…' });
    fixture.detectChanges();

    buttons()[0].click();

    expect(await answer).toBeFalse();
  });

  it('résout false avec la touche Échap (événement "cancel" du <dialog>)', async () => {
    const answer = service.confirm({ title: 'Clôturer', message: '…' });
    fixture.detectChanges();

    element.querySelector('dialog')!.dispatchEvent(new Event('cancel', { cancelable: true }));

    expect(await answer).toBeFalse();
  });

  it("annonce une erreur avec un seul bouton", async () => {
    const closed = service.alert({ title: 'Action impossible', message: 'Cet événement a des réservations.' });
    fixture.detectChanges();

    expect(buttons().map((b) => b.textContent?.trim())).toEqual(['OK']);
    expect(element.querySelector('dialog')?.getAttribute('data-tone')).toBe('error');
    buttons()[0].click();
    await expectAsync(closed).toBeResolved();
  });

  it('une nouvelle fenêtre refuse la précédente (une seule à la fois)', async () => {
    const first = service.confirm({ title: 'Première', message: '…' });
    service.confirm({ title: 'Deuxième', message: '…' });

    expect(await first).toBeFalse();
    expect(service.current()?.title).toBe('Deuxième');
  });
});
