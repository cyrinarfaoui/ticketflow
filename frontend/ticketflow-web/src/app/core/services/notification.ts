import { Injectable, signal } from '@angular/core';

export type NotificationType = 'error' | 'info';

export interface AppNotification {
  readonly text: string;
  readonly type: NotificationType;
}

@Injectable({ providedIn: 'root' })
export class NotificationService {

  // Le message en cours (null = rien à afficher). Modifiable UNIQUEMENT ici.
  private readonly current = signal<AppNotification | null>(null);

  // Version en lecture seule, exposée au reste de l'application
  readonly notification = this.current.asReadonly();

  error(text: string): void {
    this.current.set({ text, type: 'error' });
  }

  clear(): void {
    this.current.set(null);
  }
}