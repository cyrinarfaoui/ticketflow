import { Routes } from '@angular/router';
import { EventList } from './features/events/event-list/event-list';
import { EventForm } from './features/events/event-form/event-form';
import { adminGuard } from './core/guards/admin.guard';


export const routes: Routes = [

      { path: '', component: EventList },
      { path: 'events/new', component: EventForm, canActivate: [adminGuard] }

];
