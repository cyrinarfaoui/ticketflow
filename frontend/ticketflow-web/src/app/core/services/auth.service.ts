import { computed, inject, Injectable } from '@angular/core';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL } from 'keycloak-angular';

/**
 * État d'authentification global, en signals.
 * Keycloak n'est pas réactif ; KEYCLOAK_EVENT_SIGNAL change à chaque événement (connexion, déconnexion,
 * jeton expiré…) : en le lisant, chaque computed est recalculé au bon moment, et tous les écrans suivent.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {

  private readonly keycloak = inject(Keycloak);
  private readonly keycloakEvent = inject(KEYCLOAK_EVENT_SIGNAL);

  readonly isLoggedIn = computed(() => {
    this.keycloakEvent(); // dépendance : recalculé à chaque événement Keycloak
    return this.keycloak.authenticated ?? false;
  });

  readonly username = computed<string | undefined>(() => {
    this.keycloakEvent();
    return this.keycloak.tokenParsed?.['preferred_username'];
  });

  private readonly roles = computed<string[]>(() => {
    this.keycloakEvent();
    return this.keycloak.realmAccess?.roles ?? [];
  });

  // État DÉRIVÉ : calculé à partir de roles, jamais stocké ni mis à jour à la main
  readonly isAdmin = computed(() => this.roles().includes('ADMIN'));

  hasRole(role: string): boolean {
    return this.roles().includes(role);
  }

  login(): void {
    this.keycloak.login();
  }

  register(): void {
    this.keycloak.register();
  }

  logout(): void {
    this.keycloak.logout();
  }
}