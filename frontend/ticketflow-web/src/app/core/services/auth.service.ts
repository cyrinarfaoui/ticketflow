import { inject, Injectable, Service } from '@angular/core';
import Keycloak from 'keycloak-js';

@Injectable({ providedIn: 'root' })
export class AuthService {

    
  private keycloak = inject(Keycloak);

  login () : void{
    this.keycloak.login();
  }

  register(): void {
  this.keycloak.register();
}

  logout(): void{
    this.keycloak.logout();
  }

  isLoggedIn(): boolean {
   return this.keycloak.authenticated ?? false ;
  }

  getUsername (): string | undefined {
    return this.keycloak.tokenParsed?.['preferred_username'];
  }

   hasRole(role: string): boolean {
    return this.keycloak.hasRealmRole(role);
  }




}
