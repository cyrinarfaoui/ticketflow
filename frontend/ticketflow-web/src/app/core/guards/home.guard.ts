import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

/**
 * L'accueil "vitrine" (événements à la une) est fait pour les clients.
 * Un admin arrive directement sur sa vue d'ensemble : le tableau de bord.
 */
export const homeGuard: CanActivateFn = () => {
  const authService = inject(AuthService);
  return authService.hasRole('ADMIN') ? inject(Router).createUrlTree(['/admin']) : true;
};
