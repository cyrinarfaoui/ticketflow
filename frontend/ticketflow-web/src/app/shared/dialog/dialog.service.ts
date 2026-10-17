import { Injectable, signal } from '@angular/core';

export type DialogTone = 'default' | 'danger' | 'error';

export interface DialogOptions {
  title: string;
  message: string;
  confirmLabel?: string;   // texte du bouton principal ("Supprimer", "Clôturer"…)
  tone?: DialogTone;       // danger = action irréversible (bouton rouge) ; error = annonce d'une erreur
}

export interface OpenDialog {
  title: string;
  message: string;
  confirmLabel: string;
  cancelLabel: string | null;   // null = fenêtre d'information (un seul bouton)
  tone: DialogTone;
}

/**
 * Fenêtres de confirmation et d'erreur de toute l'application (remplace confirm() et alert() du navigateur).
 * Une page appelle confirm()/alert() ; le composant <app-dialog-host> (placé une fois dans app.html) l'affiche.
 */
@Injectable({ providedIn: 'root' })
export class DialogService {

  private readonly state = signal<OpenDialog | null>(null);
  readonly current = this.state.asReadonly();
  private resolveCurrent: ((confirmed: boolean) => void) | null = null;

  /** Demande une confirmation. Résout true si l'utilisateur confirme, false s'il revient en arrière (ou Échap). */
  confirm(options: DialogOptions): Promise<boolean> {
    // "Retour" et pas "Annuler" : sinon "Annuler l'événement ? [Annuler] [OK]" devient incompréhensible
    return this.open(options, 'Retour', options.tone ?? 'default');
  }

  /** Annonce une information ou une erreur (un seul bouton). */
  alert(options: DialogOptions): Promise<void> {
    return this.open(options, null, options.tone ?? 'error').then(() => undefined);
  }

  /** Appelé par la fenêtre quand l'utilisateur clique un bouton ou appuie sur Échap. */
  close(confirmed: boolean): void {
    const resolve = this.resolveCurrent;
    this.resolveCurrent = null;
    this.state.set(null);
    resolve?.(confirmed);
  }

  private open(options: DialogOptions, cancelLabel: string | null, tone: DialogTone): Promise<boolean> {
    this.close(false);   // une seule fenêtre à la fois : l'éventuelle précédente est refusée
    return new Promise<boolean>((resolve) => {
      this.resolveCurrent = resolve;
      this.state.set({
        title: options.title,
        message: options.message,
        confirmLabel: options.confirmLabel ?? 'OK',
        cancelLabel,
        tone,
      });
    });
  }
}
