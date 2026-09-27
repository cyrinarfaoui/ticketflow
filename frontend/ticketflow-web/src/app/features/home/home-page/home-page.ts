import { Component } from '@angular/core';
import { EventList } from '../../events/event-list/event-list';
import { Hero } from '../hero/hero';

@Component({
  imports: [EventList,Hero],
  selector: 'app-home-page',
  styleUrl: './home-page.scss',
  templateUrl: './home-page.html',
})
export class HomePage {}
