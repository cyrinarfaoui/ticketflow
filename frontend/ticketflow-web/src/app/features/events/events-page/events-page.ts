import { Component } from '@angular/core';
import { EventList } from '../event-list/event-list';

@Component({
  selector: 'app-events-page',
  imports: [EventList],
  templateUrl: './events-page.html',
  styleUrl: './events-page.scss',
})
export class EventsPage {}