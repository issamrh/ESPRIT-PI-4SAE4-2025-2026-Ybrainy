import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { UserService } from '../frontoffice/services/user.service';

export interface UserProfile {
  userId: number;
  username: string;
  firstName: string;
  lastName: string;
  email: string;
  role: string;
  age?: number;
  address?: string;
  city?: string;
  country?: string;
  sex?: string;
  companyName?: string;
  enterpriseCompanyId?: string;
  enterpriseVerified?: boolean;
  enterpriseOnboardedAt?: Date;
  faceBiometricHash?: string;
  streakDays?: number;
  lastLogin?: Date;
  accountCreatedAt?: Date;
  lastProfileUpdate?: Date;
  status?: 'SECURE' | 'WARNED' | 'SUSPENDED' | 'FLAGGED';
  profilePicture?: string;
  dateOfBirth?: string;
  banPeriod?: number;
  reasonForBan?: string;
  lockedUntil?: Date;
  keycloakUserId?: string;
  personalityId?: number;
}

@Component({
  selector: 'app-profile',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './profile.component.html',
  styleUrls: ['./profile.component.css']
})
export class ProfileComponent implements OnInit {

  user: UserProfile | null = null;
  saving = false;
  toastVisible = false;
  toastMessage = '';
  toastError = false;

  editModel: Partial<UserProfile> = {};
  passwordModel = { current: '', newPwd: '', confirmPwd: '' };

  constructor(
    private userService: UserService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadProfile();
  }

  loadProfile(): void {
    this.userService.getCurrentUser().subscribe({
      next: (data: UserProfile) => {
        this.user = data;
        this.editModel = {
          firstName:   data.firstName,
          lastName:    data.lastName,
          username:    data.username,
          age:         data.age,
          address:     data.address,
          city:        data.city,
          country:     data.country,
          sex:         data.sex,
          companyName: data.companyName,
          dateOfBirth: data.dateOfBirth,
        };
      },
      error: () => this.showToast('Could not load profile. Please try again.', true),
    });
  }

  getInitials(): string {
    if (!this.user) return '?';
    const f = this.user.firstName?.[0] ?? '';
    const l = this.user.lastName?.[0] ?? '';
    return (f + l).toUpperCase() || this.user.username?.[0]?.toUpperCase() ?? '?';
  }

  onAvatarChange(event: Event): void {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      if (this.user) {
        this.user.profilePicture = reader.result as string;
      }
      // TODO: upload to backend
      // this.userService.uploadAvatar(file).subscribe(...)
    };
    reader.readAsDataURL(file);
  }

  onSave(): void {
    if (!this.user) return;
    this.saving = true;

    this.userService.updateUser(this.user.userId, this.editModel).subscribe({
      next: (updated: UserProfile) => {
        this.user = { ...this.user!, ...updated };
        this.saving = false;
        this.showToast('Profile updated successfully!');
      },
      error: () => {
        this.saving = false;
        this.showToast('Failed to save changes. Please try again.', true);
      },
    });
  }

  onChangePassword(): void {
    const { current, newPwd, confirmPwd } = this.passwordModel;
    if (!current || !newPwd || !confirmPwd) {
      return this.showToast('Please fill in all password fields.', true);
    }
    if (newPwd !== confirmPwd) {
      return this.showToast('New passwords do not match.', true);
    }
    if (newPwd.length < 8) {
      return this.showToast('Password must be at least 8 characters.', true);
    }

    // TODO: wire to backend
    // this.userService.changePassword(current, newPwd).subscribe(...)
    this.passwordModel = { current: '', newPwd: '', confirmPwd: '' };
    this.showToast('Password updated successfully!');
  }

  onDeleteAccount(): void {
    if (!confirm('Are you absolutely sure you want to delete your account? This cannot be undone.')) return;
    if (!this.user) return;

    this.userService.deleteUser(this.user.userId).subscribe({
      next: () => this.router.navigate(['/login']),
      error: () => this.showToast('Could not delete account. Please contact support.', true),
    });
  }

  private showToast(message: string, isError = false): void {
    this.toastMessage = message;
    this.toastError = isError;
    this.toastVisible = true;
    setTimeout(() => (this.toastVisible = false), 3500);
  }
}
