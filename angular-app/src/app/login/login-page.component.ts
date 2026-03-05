import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.css',
  host: { style: 'display:block' },
})
export class LoginPageComponent {
  readonly loginUrl = 'assets/backoffice/page-login.html';
  readonly safeLoginUrl: SafeResourceUrl;

  constructor(private sanitizer: DomSanitizer) {
    this.safeLoginUrl = this.sanitizer.bypassSecurityTrustResourceUrl(this.loginUrl);
  }
}
