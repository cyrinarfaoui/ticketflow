import { Component, effect, ElementRef, inject, viewChild } from '@angular/core';
import { DialogService } from './dialog.service';

/**
 * Affiche la fenêtre demandée au DialogService, dans un <dialog> natif :
 * fenêtre modale (le reste de la page est inerte), focus piégé dedans, Échap pour revenir en arrière (RGAA).
 */
@Component({
  selector: 'app-dialog-host',
  templateUrl: './dialog-host.html',
  styleUrl: './dialog-host.scss',
})
export class DialogHost {
  protected readonly dialogs = inject(DialogService);
  private readonly dialog = viewChild.required<ElementRef<HTMLDialogElement>>('dialog');

  constructor() {
    // Le signal "fenêtre ouverte" pilote l'ouverture / la fermeture du <dialog>
    effect(() => {
      const element = this.dialog().nativeElement;
      const open = this.dialogs.current() !== null;
      if (open && !element.open) {
        element.showModal();
      } else if (!open && element.open) {
        element.close();
      }
    });
  }

  // Échap : on ne laisse pas le navigateur fermer seul, on passe par le service (réponse "false")
  protected onCancel(event: Event): void {
    event.preventDefault();
    this.dialogs.close(false);
  }
}
