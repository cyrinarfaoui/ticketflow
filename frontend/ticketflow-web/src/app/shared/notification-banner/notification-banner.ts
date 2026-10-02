import { Component, inject } from '@angular/core';
import { NotificationService } from '../../core/services/notification';

@Component({
  selector: 'app-notification-banner',
  imports: [],
  templateUrl: './notification-banner.html',
  styleUrl: './notification-banner.scss',
})
export class NotificationBanner {
  protected readonly notifications = inject(NotificationService);
}