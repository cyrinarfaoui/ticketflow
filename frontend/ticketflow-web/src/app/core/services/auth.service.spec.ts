import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import Keycloak from 'keycloak-js';
import { KEYCLOAK_EVENT_SIGNAL, KeycloakEvent, KeycloakEventType } from 'keycloak-angular';
import { AuthService } from './auth.service';

describe('AuthService (état en signals)', () => {
  let service: AuthService;
  // Faux Keycloak : on change ses propriétés à la main, comme le ferait le vrai lors d'une connexion
  let keycloak: {
    authenticated?: boolean;
    tokenParsed?: Record<string, unknown>;
    realmAccess?: { roles: string[] };
    login: jasmine.Spy;
    logout: jasmine.Spy;
    register: jasmine.Spy;
  };
  // Faux signal d'événements : on "émet" un événement avec events.set(...)
  let events: ReturnType<typeof signal<KeycloakEvent>>;

  beforeEach(() => {
    keycloak = {
      authenticated: false,
      login: jasmine.createSpy('login'),
      logout: jasmine.createSpy('logout'),
      register: jasmine.createSpy('register'),
    };
    events = signal<KeycloakEvent>({ type: KeycloakEventType.KeycloakAngularNotInitialized });

    TestBed.configureTestingModule({
      providers: [
        { provide: Keycloak, useValue: keycloak },
        { provide: KEYCLOAK_EVENT_SIGNAL, useValue: events },
      ],
    });
    service = TestBed.inject(AuthService);
  });

  // Simule une connexion réussie : Keycloak remplit ses propriétés PUIS émet AuthSuccess
  function loginAs(username: string, roles: string[]): void {
    keycloak.authenticated = true;
    keycloak.tokenParsed = { preferred_username: username };
    keycloak.realmAccess = { roles };
    events.set({ type: KeycloakEventType.AuthSuccess });
  }

  it('n\'est pas connecté au départ', () => {
    expect(service.isLoggedIn()).toBeFalse();
    expect(service.username()).toBeUndefined();
    expect(service.isAdmin()).toBeFalse();
  });

  it('se met à jour tout seul après une connexion (événement Keycloak)', () => {
    loginAs('cyrine', ['USER', 'ADMIN']);

    expect(service.isLoggedIn()).toBeTrue();
    expect(service.username()).toBe('cyrine');
    expect(service.isAdmin()).toBeTrue();
  });

  it('un utilisateur sans le rôle ADMIN n\'est pas admin', () => {
    loginAs('sara', ['USER']);

    expect(service.isAdmin()).toBeFalse();
    expect(service.hasRole('USER')).toBeTrue();
  });

  it('repasse à "déconnecté" après l\'événement de déconnexion', () => {
    loginAs('cyrine', ['ADMIN']);

    keycloak.authenticated = false;
    keycloak.tokenParsed = undefined;
    keycloak.realmAccess = undefined;
    events.set({ type: KeycloakEventType.AuthLogout });

    expect(service.isLoggedIn()).toBeFalse();
    expect(service.isAdmin()).toBeFalse();
  });

  it('délègue la connexion à Keycloak', () => {
    service.login();

    expect(keycloak.login).toHaveBeenCalled();
  });
});