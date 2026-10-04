import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

// Page réservée aux utilisateurs connectés (n'importe quel rôle).
// C'est du confort d'affichage : la vraie protection, c'est le backend qui refuse sans jeton valide.
export const authGuard: CanActivateFn = () => {
  if (inject(AuthService).isLoggedIn()) {
    return true;
  }
  return inject(Router).createUrlTree(['/']);
};
