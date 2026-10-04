import { Routes } from '@angular/router';
import { EventForm } from './features/events/event-form/event-form';
import { adminGuard } from './core/guards/admin.guard';
import { HomePage } from './features/home/home-page/home-page';
import { EventsPage } from './features/events/events-page/events-page';


export const routes: Routes = [

  { path: '', component: HomePage },
  { path: 'evenements', component: EventsPage },
  { path: 'events/new', component: EventForm, canActivate: [adminGuard] },
  {
    path: 'admin',
    canActivate: [adminGuard],
    // Chargement à la demande (lazy loading) : le code du dashboard n'est téléchargé
    // que lorsqu'un admin ouvre la page, pas par tous les visiteurs
    loadComponent: () => import('./features/admin/admin-dashboard/admin-dashboard').then((m) => m.AdminDashboard),
    title: 'Tableau de bord – TicketFlow',
  },

];
