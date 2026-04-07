import { Component, HostListener, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Subject, takeUntil } from 'rxjs';
import { AuthMockService, MockUser } from '../../services/auth-mock.service';

@Component({
  selector: 'app-user-switcher',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-switcher.component.html',
  styleUrls: ['./user-switcher.component.css'],
})
export class UserSwitcherComponent implements OnInit, OnDestroy {
  private readonly destroy$ = new Subject<void>();

  visible = false;
  currentUser: MockUser | null;
  readonly users: readonly MockUser[];

  constructor(private authMock: AuthMockService) {
    this.users = authMock.users;
    this.currentUser = authMock.currentUser();
  }

  ngOnInit(): void {
    this.authMock.currentUser$
      .pipe(takeUntil(this.destroy$))
      .subscribe(u => (this.currentUser = u));
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  @HostListener('window:keydown', ['$event'])
  onKeydown(e: KeyboardEvent): void {
    if (e.ctrlKey && e.key === 'u') {
      e.preventDefault();
      this.visible = !this.visible;
    }
  }

  switch(id: number): void {
    this.authMock.switchUser(id);
  }

  roleClass(role: string): string {
    return `us-badge-${role.toLowerCase()}`;
  }
}
