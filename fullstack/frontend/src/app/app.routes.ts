import { Routes } from '@angular/router';
import { LoginComponent } from './login.component';
import { DashboardComponent } from './dashboard.component';
import { SettingsComponent } from './settings.component';
import { DispatcherDashboardComponent } from './dispatcher-dashboard.component';
import { MessageCenterComponent } from './message-center.component';
import { AdminControlComponent } from './admin-control.component';
import { ObservabilityComponent } from './observability.component';
import { authGuard } from './auth.guard';
import { roleGuard } from './role.guard';

export const appRoutes: Routes = [
  { path: '', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
  { path: 'settings', component: SettingsComponent, canActivate: [authGuard, roleGuard], data: { roles: ['PASSENGER'] } },
  { path: 'dispatcher', component: DispatcherDashboardComponent, canActivate: [authGuard, roleGuard], data: { roles: ['DISPATCHER', 'ADMIN'] } },
  { path: 'messages', component: MessageCenterComponent, canActivate: [authGuard, roleGuard], data: { roles: ['PASSENGER'] } },
  { path: 'admin', component: AdminControlComponent, canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } },
  { path: 'observability', component: ObservabilityComponent, canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } },
  { path: '**', redirectTo: '' }
];
