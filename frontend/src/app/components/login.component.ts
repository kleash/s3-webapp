import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="login-card">
      <h1>S3 Browser</h1>
      <p class="subtitle">Sign in with your LDAP username and password.</p>
      <form (ngSubmit)="login()" #loginForm="ngForm">
        <label>Username</label>
        <input name="username" [(ngModel)]="username" required autocomplete="username" />

        <label>Password</label>
        <input type="password" name="password" [(ngModel)]="password" required autocomplete="current-password" />

        <button type="submit" [disabled]="loading || !username || !password">Login</button>
      </form>

      <div class="error" *ngIf="error">{{ error }}</div>
      <div class="hint">
        <p>Dev/preview users:</p>
        <ul>
          <li><strong>alice</strong> / password1 — Read-only</li>
          <li><strong>bob</strong> / password1 — Read & write</li>
        </ul>
      </div>
    </div>
  `,
  styles: [`
    :host { display: flex; justify-content: center; padding: 3rem 1rem; }
    .login-card { background: #0b1c2c; color: #f7faff; padding: 2rem; border-radius: 16px; width: 100%; max-width: 420px; box-shadow: 0 25px 60px rgba(0,0,0,0.35); }
    h1 { margin: 0 0 0.25rem; }
    .subtitle { margin: 0 0 1rem; color: #b4c5d6; }
    form { display: flex; flex-direction: column; gap: 0.75rem; }
    label { color: #9fb3c6; font-size: 0.9rem; }
    input { width: 100%; padding: 0.55rem 0.7rem; border-radius: 10px; border: 1px solid #2d4053; background: rgba(255,255,255,0.08); color: #fff; }
    button { padding: 0.65rem; border: none; border-radius: 10px; background: linear-gradient(135deg, #14b8ff, #5eead4); color: #0b1c2c; font-weight: 700; cursor: pointer; }
    button:disabled { opacity: 0.6; cursor: not-allowed; }
    .error { margin-top: 0.75rem; color: #ff9fb1; }
    .hint { margin-top: 1rem; font-size: 0.9rem; color: #c5d7e8; }
    .hint ul { margin: 0.35rem 0 0; padding-left: 1.1rem; }
  `]
})
export class LoginComponent {
  username = '';
  password = '';
  error = '';
  loading = false;

  constructor(private auth: AuthService, private router: Router) {}

  login() {
    if (!this.username || !this.password) {
      return;
    }
    this.error = '';
    this.loading = true;
    this.auth.login(this.username, this.password).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigateByUrl('/');
      },
      error: () => {
        this.error = 'Invalid username or password';
        this.loading = false;
      }
    });
  }
}
