import { inject } from '@angular/core';
import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { NotificationService } from '../services/notification';

/**
 * Intercepte toutes les réponses HTTP en erreur :
 * - erreurs techniques (0, 401, 403, 5xx) → message global pour l'utilisateur
 * - erreurs métier (400, 404, 409…) → rien ici, le composant les gère
 * Dans tous les cas, l'erreur est relancée vers le composant.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const notifications = inject(NotificationService);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      const message = messageFor(error);
      if (message) {
        notifications.error(message);
      }
      return throwError(() => error);
    }),
  );
};

// Message global à afficher, ou null si l'erreur est métier (gérée par le composant)
function messageFor(error: HttpErrorResponse): string | null {
  if (error.status === 0) {
    return 'Impossible de joindre le serveur. Vérifiez votre connexion.';
  }
  if (error.status === 401) {
    return 'Votre session a expiré. Connectez-vous pour continuer.';
  }
  if (error.status === 403) {
    return 'Vous n\'avez pas les droits nécessaires pour cette action.';
  }
  if (error.status >= 500) {
    return 'Une erreur technique est survenue. Réessayez plus tard.';
  }
  return null;
}