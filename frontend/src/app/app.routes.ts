import { Routes } from '@angular/router';

import { authGuard } from './core/theme/auth/auth.guard';
import { lockedGuard } from './core/theme/auth/locked.guard';

const placeholder = () => import('./features/placeholder/placeholder').then(m => m.Placeholder);

export const routes: Routes = [
  { path: '', redirectTo: 'signin', pathMatch: 'full' },
  { path: 'signin', canDeactivate: [lockedGuard], loadComponent: () => import('./features/sign-in/sign-in').then(m => m.SignIn) },
  {
    // Everything signed-in lives inside the layout; only /signin is outside it.
    path: '',canActivate: [authGuard],loadComponent: () => import('./layout/main-layout').then(m => m.MainLayout),
    children: [
      { path: 'home', loadComponent: () => import('./features/home/home').then(m => m.Home) },
      { path: 'tickets', loadComponent: () => import('./features/my-tickets/my-tickets').then(m => m.MyTickets) },
      { path: 'tickets/:id', loadComponent: () => import('./features/ticket-workspace/ticket-workspace').then(m => m.TicketWorkspace) },
      { path: 'customer-search', loadComponent: () => import('./features/customer-search/customer-search').then(m => m.CustomerSearch) },
      { path: 'customer-search/:cif', loadComponent: () => import('./features/customer-360/customer-360').then(m => m.Customer360) },
      { path: 'customer-search/:cif/new-ticket', loadComponent: () => import('./features/new-ticket/new-ticket').then(m => m.NewTicket) },
      { path: 'review', loadComponent: placeholder, data: { title: 'Review queue' } },
      { path: 'library', loadComponent: placeholder, data: { title: 'Guide library' } },
      { path: 'triage', loadComponent: placeholder, data: { title: 'Triage' } },
      { path: 'feedback', loadComponent: placeholder, data: { title: 'Agent feedback' } },
      { path: 'analytics', loadComponent: placeholder, data: { title: 'Guide analytics' } },
      { path: 'rules', loadComponent: placeholder, data: { title: 'Error rules & jobs' } },
      { path: 'audit', loadComponent: placeholder, data: { title: 'Audit log' } },
      { path: 'alerts', loadComponent: placeholder, data: { title: 'Notifications' } },
      
    ],
  },
  { path: '**', redirectTo: 'signin' },
];
