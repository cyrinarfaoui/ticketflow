import { Routes } from '@angular/router';
import { EventList } from './features/events/event-list/event-list';
import { EventForm } from './features/events/event-form/event-form';
import { adminGuard } from './core/guards/admin.guard';
import { HomePage } from './features/home/home-page/home-page';
import { EventsPage } from './features/events/events-page/events-page';


export const routes: Routes = [
       
  { path: '', component: HomePage },
  { path: 'evenements', component: EventsPage },
  { path: 'events/new', component: EventForm, canActivate: [adminGuard] },

];
