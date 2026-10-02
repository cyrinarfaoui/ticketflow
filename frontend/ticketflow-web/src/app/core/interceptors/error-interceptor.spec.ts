import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { NotificationService } from '../services/notification';
import { errorInterceptor } from './error-interceptor';

describe('errorInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let notifications: NotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    notifications = TestBed.inject(NotificationService);
  });

  afterEach(() => httpMock.verify());

  // Envoie une requête et mémorise l'erreur que le COMPOSANT recevrait
  function sendRequest(): { error?: HttpErrorResponse } {
    const result: { error?: HttpErrorResponse } = {};
    http.get('/api/test').subscribe({ error: (e) => (result.error = e) });
    return result;
  }

  function respondWithStatus(status: number, body: object | null = null): void {
    httpMock.expectOne('/api/test').flush(body, { status, statusText: 'Erreur' });
  }

  it('affiche un message quand le serveur est injoignable (statut 0)', () => {
    sendRequest();
    httpMock.expectOne('/api/test').error(new ProgressEvent('error'));

    expect(notifications.notification()?.text).toBe('Impossible de joindre le serveur. Vérifiez votre connexion.');
  });

  it('affiche un message de session expirée (401)', () => {
    sendRequest();
    respondWithStatus(401);

    expect(notifications.notification()?.text).toBe('Votre session a expiré. Connectez-vous pour continuer.');
  });

  it('affiche un message de droits insuffisants (403)', () => {
    sendRequest();
    respondWithStatus(403);

    expect(notifications.notification()?.text).toBe('Vous n\'avez pas les droits nécessaires pour cette action.');
  });

  it('affiche un message d\'erreur technique (500 et plus)', () => {
    sendRequest();
    respondWithStatus(503);

    expect(notifications.notification()).toEqual({
      text: 'Une erreur technique est survenue. Réessayez plus tard.',
      type: 'error',
    });
  });

  it('laisse les erreurs métier (409) au composant, sans message global', () => {
    const result = sendRequest();
    respondWithStatus(409, { error: 'Pas assez de places disponibles' });

    expect(notifications.notification()).toBeNull();
    expect(result.error?.error).toEqual({ error: 'Pas assez de places disponibles' });
  });

  it('transmet toujours l\'erreur au composant après l\'avoir traitée', () => {
    const result = sendRequest();
    respondWithStatus(500);

    expect(result.error?.status).toBe(500);
  });

  it('ne fait rien quand la requête réussit', () => {
    http.get('/api/test').subscribe();
    httpMock.expectOne('/api/test').flush([]);

    expect(notifications.notification()).toBeNull();
  });
});