import { TestBed } from '@angular/core/testing';
import { API_BASE_URL } from '../config/api.config';
import { EVENT_SOURCE_FACTORY } from '../config/event-source';
import { LiveNotification } from '../models/live-notification';
import { EventService } from '../services/event.service';
import { LiveNotificationStore } from './live-notification.store';

/** Faux EventSource : on déclenche nous-mêmes l'ouverture, les messages et les coupures. */
class FakeEventSource {
  readyState = 0;   // 0 = CONNECTING, 1 = OPEN, 2 = CLOSED (mêmes valeurs que EventSource)
  onopen: (() => void) | null = null;
  onerror: (() => void) | null = null;
  closed = false;
  private readonly listeners = new Map<string, (message: MessageEvent) => void>();

  constructor(readonly url: string) {}

  addEventListener(type: string, listener: (message: MessageEvent) => void): void {
    this.listeners.set(type, listener);
  }

  close(): void {
    this.closed = true;
    this.readyState = 2;
  }

  // ---- Méthodes de test ----
  simulateOpen(): void {
    this.readyState = 1;
    this.onopen?.();
  }

  simulateMessage(type: string, data: string): void {
    this.listeners.get(type)?.(new MessageEvent(type, { data }));
  }

  simulateError(readyState: number): void {
    this.readyState = readyState;
    this.onerror?.();
  }
}

function notification(overrides: Partial<LiveNotification> = {}): LiveNotification {
  return {
    eventId: 'e1', eventTitle: 'Concert de jazz', seats: 2, availableSeats: 41,
    message: '2 place(s) réservée(s) pour « Concert de jazz » — il en reste 41',
    ...overrides,
  };
}

describe('LiveNotificationStore', () => {
  let store: LiveNotificationStore;
  let sources: FakeEventSource[];
  let eventService: jasmine.SpyObj<EventService>;

  const lastSource = () => sources[sources.length - 1];

  beforeEach(() => {
    sources = [];
    eventService = jasmine.createSpyObj<EventService>('EventService', ['updateAvailableSeats']);
    TestBed.configureTestingModule({
      providers: [
        {
          provide: EVENT_SOURCE_FACTORY,
          useValue: (url: string) => {
            const source = new FakeEventSource(url);
            sources.push(source);
            return source as unknown as EventSource;
          },
        },
        { provide: EventService, useValue: eventService },
      ],
    });
    store = TestBed.inject(LiveNotificationStore);
  });

  afterEach(() => store.disconnect());

  it('ouvre le flux SSE de la gateway', () => {
    store.connect();

    expect(sources.length).toBe(1);
    expect(lastSource().url).toBe(`${API_BASE_URL}/notifications/stream`);
    expect(store.status()).toBe('connecting');
  });

  it("n'ouvre pas un deuxième flux si on appelle connect() deux fois", () => {
    store.connect();
    store.connect();

    expect(sources.length).toBe(1);
  });

  it('passe à "open" quand la connexion est établie', () => {
    store.connect();
    lastSource().simulateOpen();

    expect(store.status()).toBe('open');
  });

  it('ajoute chaque notification en tête de liste et compte les non-lues', () => {
    store.connect();
    lastSource().simulateMessage('reservation', JSON.stringify(notification({ message: 'première' })));
    lastSource().simulateMessage('reservation', JSON.stringify(notification({ message: 'seconde' })));

    expect(store.notifications().map((n) => n.message)).toEqual(['seconde', 'première']);
    expect(store.unreadCount()).toBe(2);
  });

  it('ne garde que les 10 notifications les plus récentes', () => {
    store.connect();
    for (let i = 1; i <= 12; i++) {
      lastSource().simulateMessage('reservation', JSON.stringify(notification({ message: `n${i}` })));
    }

    expect(store.notifications().length).toBe(LiveNotificationStore.MAX_ITEMS);
    expect(store.notifications()[0].message).toBe('n12');
  });

  it("remet le compteur à zéro quand l'utilisateur a tout lu", () => {
    store.connect();
    lastSource().simulateMessage('reservation', JSON.stringify(notification()));

    store.markAllRead();

    expect(store.unreadCount()).toBe(0);
    expect(store.notifications().length).toBe(1);   // la liste, elle, reste
  });

  it("met à jour en direct les places de l'événement dans le catalogue", () => {
    store.connect();
    lastSource().simulateMessage('reservation', JSON.stringify(notification({ eventId: 'e7', availableSeats: 12 })));

    expect(eventService.updateAvailableSeats).toHaveBeenCalledOnceWith('e7', 12);
  });

  it('ne touche pas au catalogue quand le nombre de places est inconnu (notification simple)', () => {
    store.connect();
    lastSource().simulateMessage('reservation', JSON.stringify(notification({ eventTitle: null, availableSeats: null })));

    expect(eventService.updateAvailableSeats).not.toHaveBeenCalled();
    expect(store.notifications().length).toBe(1);
  });

  it('ignore un message illisible sans casser le flux', () => {
    store.connect();
    lastSource().simulateMessage('reservation', 'pas du JSON');
    lastSource().simulateMessage('reservation', JSON.stringify(notification()));

    expect(store.notifications().length).toBe(1);
  });

  it('coupure : laisse le navigateur se reconnecter tout seul (état CONNECTING)', () => {
    store.connect();
    lastSource().simulateOpen();

    lastSource().simulateError(0);   // CONNECTING : le navigateur réessaie lui-même

    expect(store.status()).toBe('reconnecting');
    expect(sources.length).toBe(1);  // on n'ouvre PAS de deuxième flux
  });

  describe('quand le navigateur abandonne (état CLOSED)', () => {
    beforeEach(() => jasmine.clock().install());
    afterEach(() => jasmine.clock().uninstall());

    it('rouvre un flux après le délai, puis repasse à "open"', () => {
      store.connect();
      lastSource().simulateError(2);

      jasmine.clock().tick(LiveNotificationStore.RETRY_DELAY_MS - 1);
      expect(sources.length).toBe(1);                 // pas encore

      jasmine.clock().tick(1);
      expect(sources.length).toBe(2);                 // nouveau flux
      expect(store.status()).toBe('reconnecting');

      lastSource().simulateOpen();
      expect(store.status()).toBe('open');
    });

    it('disconnect() annule la reconnexion prévue', () => {
      store.connect();
      lastSource().simulateError(2);

      store.disconnect();
      jasmine.clock().tick(LiveNotificationStore.RETRY_DELAY_MS);

      expect(sources.length).toBe(1);
    });
  });

  it('disconnect() ferme le flux', () => {
    store.connect();
    const source = lastSource();

    store.disconnect();

    expect(source.closed).toBeTrue();
  });
});
