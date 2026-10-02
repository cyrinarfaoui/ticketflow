import { TestBed } from '@angular/core/testing';
import { NotificationService } from './notification';

describe('NotificationService', () => {
  let service: NotificationService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(NotificationService);
  });

  it('n\'affiche aucune notification au démarrage', () => {
    expect(service.notification()).toBeNull();
  });

  it('mémorise un message d\'erreur', () => {
    service.error('Le serveur est injoignable');

    expect(service.notification()).toEqual({ text: 'Le serveur est injoignable', type: 'error' });
  });

  it('remplace le message précédent par le nouveau', () => {
    service.error('Premier message');
    service.error('Second message');

    expect(service.notification()?.text).toBe('Second message');
  });

  it('efface la notification', () => {
    service.error('Le serveur est injoignable');

    service.clear();

    expect(service.notification()).toBeNull();
  });
});