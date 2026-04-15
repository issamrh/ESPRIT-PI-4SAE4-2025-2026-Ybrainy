import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../frontoffice/services/auth.service';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.css',
  host: { style: 'display:block' },
})
export class LoginPageComponent {
  username = '';
  password = '';
  loading = false;
  error: string | null = null;

  constructor(private auth: AuthService, private router: Router) {
    if (this.auth.isLoggedIn()) {
      this.router.navigate(['/forum']);
    }
  }

  submit(): void {
    this.error = null;
    const u = this.username.trim();
    const p = this.password;
    if (!u || !p) {
      this.error = 'Please enter your username and password.';
      return;
    }
    this.loading = true;
    this.auth.login(u, p).subscribe({
      next: () => {
        this.loading = false;
        this.router.navigate(['/forum']);
      },
      error: (err) => {
        this.loading = false;
        this.error = err?.error?.error ?? err?.error?.message ?? 'Invalid credentials. Please try again.';
      },
    });
  }
}
