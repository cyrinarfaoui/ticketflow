import { Component } from '@angular/core';
import { EventList } from '../../events/event-list/event-list';

@Component({
  imports: [EventList],
  selector: 'app-home-page',
  styleUrl: './home-page.scss',
  templateUrl: './home-page.html',
})
export class HomePage {}
