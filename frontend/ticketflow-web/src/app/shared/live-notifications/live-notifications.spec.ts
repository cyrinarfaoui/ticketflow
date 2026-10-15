import { signal, WritableSignal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LiveNotification, StreamStatus } from '../../core/models/live-notification';
import { LiveNotificationStore } from '../../core/stores/live-notification.store';
import { LiveNotifications } from './live-notifications';

describe('LiveNotifications', () => {
  let fixture: ComponentFixture<LiveNotifications>;
  let element: HTMLElement;
  // Faux store : le composant ne fait que LIRE des signals et appeler markAllRead()
  let store: {
    notifications: WritableSignal<LiveNotification[]>;
    unreadCount: WritableSignal<number>;
    status: WritableSignal<StreamStatus>;
    markAllRead: jasmine.Spy;
  };

  const notification = (message: string): LiveNotification =>
    ({ eventId: 'e1', eventTitle: 'Concert', seats: 1, availableSeats: 9, message });

  beforeEach(() => {
    store = {
      notifications: signal([]),
      unreadCount: signal(0),
      status: signal('open'),
      markAllRead: jasmine.createSpy('markAllRead'),
    };
    TestBed.configureTestingModule({
      imports: [LiveNotifications],
      providers: [{ provide: LiveNotificationStore, useValue: store }],
    });
    fixture = TestBed.createComponent(LiveNotifications);
    element = fixture.nativeElement;
    fixture.detectChanges();
  });

  const bell = () => element.querySelector<HTMLButtonElement>('.bell')!;

  function render(): void {
    fixture.detectChanges();
  }

  it("n'affiche pas de badge quand il n'y a rien de nouveau", () => {
    expect(element.querySelector('.bell-badge')).toBeNull();
  });

  it("affiche le nombre de nouvelles notifications, lisible par un lecteur d'écran", () => {
    store.unreadCount.set(3);
    render();

    expect(element.querySelector('.bell-badge')?.textContent).toContain('3 nouvelle(s)');
  });

  it('ouvre la liste au clic, la marque comme lue et met à jour aria-expanded', () => {
    store.notifications.set([notification('B'), notification('A')]);
    render();

    bell().click();
    render();

    expect(bell().getAttribute('aria-expanded')).toBe('true');
    expect(store.markAllRead).toHaveBeenCalled();
    expect([...element.querySelectorAll('.live-panel li')].map((li) => li.textContent?.trim())).toEqual(['B', 'A']);
  });

  it('referme la liste avec la touche Échap', () => {
    bell().click();
    render();

    element.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));
    render();

    expect(element.querySelector('.live-panel')).toBeNull();
    expect(bell().getAttribute('aria-expanded')).toBe('false');
  });

  it('affiche un état vide', () => {
    bell().click();
    render();

    expect(element.querySelector('.live-empty')?.textContent).toContain('Aucune activité');
  });

  it("annonce la dernière notification aux lecteurs d'écran (aria-live)", () => {
    store.notifications.set([notification('2 place(s) réservée(s) pour « Concert »')]);
    render();

    expect(element.querySelector('[aria-live="polite"]')?.textContent).toContain('2 place(s) réservée(s)');
  });

  it('signale la reconnexion en cours', () => {
    store.status.set('reconnecting');
    render();

    expect(element.querySelector('.live-status')?.textContent).toContain('Reconnexion');
  });
});
