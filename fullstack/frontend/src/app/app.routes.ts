import { Routes } from '@angular/router';
import { LoginComponent } from './login.component';
import { DashboardComponent } from './dashboard.component';
import { SettingsComponent } from './settings.component';
import { DispatcherDashboardComponent } from './dispatcher-dashboard.component';
import { MessageCenterComponent } from './message-center.component';
import { AdminControlComponent } from './admin-control.component';
import { ObservabilityComponent } from './observability.component';

export const appRoutes: Routes = [
  { path: '', component: LoginComponent },
  { path: 'dashboard', component: DashboardComponent },
  { path: 'settings', component: SettingsComponent },
  { path: 'dispatcher', component: DispatcherDashboardComponent },
  { path: 'messages', component: MessageCenterComponent },
  { path: 'admin', component: AdminControlComponent },
  { path: 'observability', component: ObservabilityComponent },
  { path: '**', redirectTo: '' }
];
