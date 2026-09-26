import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const adminGuard: CanActivateFn = () => {
  const authService = inject(AuthService);

  if (authService.isLoggedIn() && authService.hasRole('ADMIN')) {
    return true;
  }

  return inject(Router).createUrlTree(['/']);
};