import { Routes } from '@angular/router';
import { LoginComponent } from './components/login.component';
import { BrowserComponent } from './components/browser.component';
import { authGuard } from './services/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', component: BrowserComponent, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
