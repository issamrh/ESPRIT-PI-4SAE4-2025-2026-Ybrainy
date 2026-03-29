import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { InteractionTrackingService } from './tracking/interaction-tracking.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent {
  title = 'angular-app';
  private readonly tracking = inject(InteractionTrackingService);

  constructor() {
    this.tracking.init();
  }
}
