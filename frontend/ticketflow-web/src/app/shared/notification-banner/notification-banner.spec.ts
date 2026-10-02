import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NotificationBanner } from './notification-banner';
import { NotificationService } from '../../core/services/notification';

describe('NotificationBanner', () => {
  let fixture: ComponentFixture<NotificationBanner>;
  let element: HTMLElement;
  let notifications: NotificationService;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NotificationBanner],
    }).compileComponents();

    notifications = TestBed.inject(NotificationService);
    fixture = TestBed.createComponent(NotificationBanner);
    element = fixture.nativeElement;
    fixture.detectChanges();
  });

  it('n\'affiche rien tant qu\'il n\'y a pas de notification', () => {
    expect(element.querySelector('[role="alert"]')).toBeNull();
  });

  it('affiche le message dans une zone d\'alerte', () => {
    notifications.error('Le serveur est injoignable');
    fixture.detectChanges();

    expect(element.querySelector('[role="alert"]')?.textContent).toContain('Le serveur est injoignable');
  });

  it('efface le message au clic sur le bouton Fermer', () => {
    notifications.error('Le serveur est injoignable');
    fixture.detectChanges();

    element.querySelector<HTMLButtonElement>('button[aria-label="Fermer le message"]')!.click();
    fixture.detectChanges();

    expect(notifications.notification()).toBeNull();
    expect(element.querySelector('[role="alert"]')).toBeNull();
  });
});