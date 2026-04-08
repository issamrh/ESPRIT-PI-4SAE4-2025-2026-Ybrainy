import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { InteractionTrackingService } from './tracking/interaction-tracking.service';
import { getDisplayName, isAuthenticated } from './auth/keycloak.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'angular-app';

  constructor(tracking: InteractionTrackingService) {
    tracking.init();

    if (isAuthenticated() && !sessionStorage.getItem('welcomeShown')) {
      sessionStorage.setItem('welcomeShown', '1');
      const name = getDisplayName() ?? 'Student';
      setTimeout(() => {
        window.alert(`Welcome ${name}`);
      }, 0);
    }
  }
}
