import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { InteractionTrackingService } from './tracking/interaction-tracking.service';
import { Router } from '@angular/router';
import { getDisplayName, getRealmRoles, isAuthenticated } from './auth/keycloak.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'angular-app';

  constructor(tracking: InteractionTrackingService, router: Router) {
    tracking.init();

    if (isAuthenticated()) {
      const roles = getRealmRoles().map((r) => r.toUpperCase());
      const path = window.location.pathname;
      if (roles.includes('ADMIN') && (path === '/' || path === '')) {
        router.navigateByUrl('/dashboard');
      }
    }

    if (isAuthenticated() && !sessionStorage.getItem('welcomeShown')) {
      sessionStorage.setItem('welcomeShown', '1');
      const name = getDisplayName() ?? 'Student';
      setTimeout(() => {
        window.alert(`Welcome ${name}`);
      }, 0);
    }
  }
}
