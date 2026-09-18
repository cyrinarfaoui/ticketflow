import { Routes } from '@angular/router';
import { EventList } from './features/events/event-list/event-list';
import { EventForm } from './features/events/event-form/event-form';

export const routes: Routes = [

      { path: '', component: EventList },
      { path: 'events/new', component: EventForm }

];
