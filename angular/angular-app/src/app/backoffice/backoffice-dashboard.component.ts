import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Subscription, combineLatest } from 'rxjs';
import { getDisplayName, logout } from '../auth/keycloak.service';

@Component({
  selector: 'app-backoffice-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './backoffice-dashboard.component.html',
  styleUrl: './backoffice-dashboard.component.css',
  host: { style: 'display:block' },
})
export class BackofficeDashboardComponent implements OnInit, OnDestroy {
  dashboardUrl: string = 'assets/backoffice/index.html';
  safeDashboardUrl: SafeResourceUrl;
  adminDisplayName = 'Admin';
  private readonly sub = new Subscription();

  private readonly allowedPages = new Set(['index.html', 'courses.html', 'lessons.html', 'app-calender.html']);

  constructor(
    private sanitizer: DomSanitizer,
    private route: ActivatedRoute
  ) {
    this.safeDashboardUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.dashboardUrl);
  }

  ngOnInit(): void {
    this.adminDisplayName = getDisplayName() ?? 'Admin';
    this.sub.add(
      combineLatest([this.route.data, this.route.queryParamMap]).subscribe(([data, qpm]) => {
        const page = (data?.['page'] as string | undefined) ?? 'index.html';
        const safePage = this.allowedPages.has(page) ? page : 'index.html';
        this.dashboardUrl = `assets/backoffice/${safePage}`;
        this.safeDashboardUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.dashboardUrl);
      })
    );
  }

  ngOnDestroy(): void {
    this.sub.unsubscribe();
  }

  async onLogout(): Promise<void> {
    await logout();
  }
}


