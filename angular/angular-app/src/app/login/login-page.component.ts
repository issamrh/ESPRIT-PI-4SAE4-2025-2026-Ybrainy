import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { UserSessionService } from '../tracking/user-session.service';
import { getKeycloak, loginWithGoogle, signInWithFaceBiometric, signInWithPassword } from '../auth/keycloak.service';
import { FaceCaptureDialogComponent } from '../shared/face-capture-dialog/face-capture-dialog.component';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink, FaceCaptureDialogComponent],
  templateUrl: './login-page.component.html',
  styleUrl: './login-page.component.css',
  host: { style: 'display:block' },
})
export class LoginPageComponent {
  private static readonly FACE_SCAN_INTERVAL_MS = 1500;

  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly session = inject(UserSessionService);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  submitting = false;
  errorMessage = '';
  showPassword = false;
  faceDialogOpen = false;
  faceSubmitting = false;
  readonly faceScanMessage = `Live face login is running. A new frame is checked every ${LoginPageComponent.FACE_SCAN_INTERVAL_MS / 1000} seconds.`;

  async onSubmit(): Promise<void> {
    this.errorMessage = '';
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.submitting) return;

    this.submitting = true;

    try {
      const { email, password } = this.form.getRawValue();
      const auth = await signInWithPassword(email, password);
      const role = String(auth.role ?? '').toUpperCase() || 'INSTRUCTOR';

      this.session.set({
        userId: typeof auth.userId === 'number' ? auth.userId : Number.NaN,
        role,
        email: auth.email ?? email,
        username: auth.username,
      });

      await this.navigateAfterLogin(role);
    } catch (error) {
      this.errorMessage = error instanceof Error ? error.message : 'Login failed. Please try again.';
    } finally {
      this.submitting = false;
    }
  }

  onGoogleLogin(): void {
    this.errorMessage = '';
    loginWithGoogle(`${window.location.origin}/`).catch(() => {
      this.errorMessage = 'Unable to start login. Please try again.';
    });
  }

  onFaceIdLogin(): void {
    this.errorMessage = '';
    this.faceDialogOpen = true;
  }

  closeFaceDialog(): void {
    if (this.faceSubmitting) {
      return;
    }
    this.faceDialogOpen = false;
  }

  async onFaceImageConfirmed(image: Blob): Promise<void> {
    if (this.faceSubmitting) {
      return;
    }

    this.errorMessage = '';
    this.faceSubmitting = true;

    try {
      await signInWithFaceBiometric(image, `${window.location.origin}/`);
    } catch (error) {
      const message =
        error instanceof Error ? error.message : 'Face sign-in failed. Please try again.';

      if (this.isRetryableFaceSignInError(message)) {
        return;
      }

      this.faceDialogOpen = false;
      this.errorMessage = message;
    } finally {
      this.faceSubmitting = false;
    }
  }

  async ngOnInit(): Promise<void> {
    try {
      const kc = getKeycloak();
      if (!kc?.authenticated || !kc.tokenParsed) return;

      const parsed: any = kc.tokenParsed;
      const roles: string[] = parsed?.realm_access?.roles ?? [];
      const role = (roles[0] ?? '').toUpperCase();

      this.session.set({
        userId: Number.NaN,
        role,
        email: parsed?.email,
        username: parsed?.preferred_username,
      });

      await this.navigateAfterLogin(role);
    } catch {
      // ignore
    }
  }

  private async navigateAfterLogin(role: string): Promise<void> {
    const redirect = this.route.snapshot.queryParamMap.get('redirect');
    if (redirect) {
      try {
        const normalized = new URL(redirect, window.location.origin);
        if (normalized.origin === window.location.origin) {
          const path = `${normalized.pathname}${normalized.search}${normalized.hash}`;
          await this.router.navigateByUrl(path || '/');
          return;
        }
      } catch {
        if (redirect.startsWith('/')) {
          await this.router.navigateByUrl(redirect);
          return;
        }
      }
    }

    await this.router.navigateByUrl(role === 'ADMIN' ? '/dashboard' : '/');
  }

  private isRetryableFaceSignInError(message: string): boolean {
    const normalized = message.toLowerCase();
    return normalized.includes('face not recognized') || normalized.includes('no face');
  }
}
