import { Routes } from '@angular/router';
import { adminGuard } from './core/guards/admin.guard';
import { authGuard } from './core/guards/auth.guard';
import { homeGuard } from './core/guards/home.guard';
import { HomePage } from './features/home/home-page/home-page';
import { EventsPage } from './features/events/events-page/events-page';


export const routes: Routes = [

  { path: '', component: HomePage, canActivate: [homeGuard] },
  { path: 'evenements', component: EventsPage },
  {
    path: 'mes-reservations',
    canActivate: [authGuard],
    loadComponent: () => import('./features/reservations/my-reservations/my-reservations').then((m) => m.MyReservations),
    title: 'Mes réservations – TicketFlow',
  },
  {
    path: 'admin/evenements',
    canActivate: [adminGuard],
    loadComponent: () => import('./features/admin/event-admin/event-admin').then((m) => m.EventAdmin),
    title: 'Gestion des événements – TicketFlow',
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    // Chargement à la demande (lazy loading) : le code du dashboard n'est téléchargé
    // que lorsqu'un admin ouvre la page, pas par tous les visiteurs
    loadComponent: () => import('./features/admin/admin-dashboard/admin-dashboard').then((m) => m.AdminDashboard),
    title: 'Tableau de bord – TicketFlow',
  },

];
