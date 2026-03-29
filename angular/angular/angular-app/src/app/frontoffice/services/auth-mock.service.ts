import { Injectable, NgZone, OnDestroy } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Router } from '@angular/router';
import { UserSessionService } from '../../tracking/user-session.service';
import { getDisplayName, getRealmRoles, isAuthenticated, logout as appLogout } from '../../auth/keycloak.service';

export interface MockUser {
  id: number;
  name: string;
  role: 'STUDENT' | 'INSTRUCTOR' | 'ADMIN' | 'ENTERPRISE_USER' | string;
}

const MOCK_USERS: MockUser[] = [
  { id: 1, name: 'Alice (Student)', role: 'STUDENT' },
  { id: 2, name: 'Bob (Student)', role: 'STUDENT' },
  { id: 100, name: 'John (Instructor)', role: 'INSTRUCTOR' },
  { id: 101, name: 'Sarah (Instructor)', role: 'INSTRUCTOR' },
  { id: 999, name: 'Admin', role: 'ADMIN' },
];

@Injectable({ providedIn: 'root' })
export class AuthMockService implements OnDestroy {
  private readonly _current$ = new BehaviorSubject<MockUser | null>(null);
  private readonly storageHandler = (event: StorageEvent) => {
    if (!event.key || event.key === 'ybrainy_mock_user' || event.key === 'bb_user_session_v1') {
      this.refreshCurrentUser();
    }
  };

  constructor(
    private router: Router,
    private zone: NgZone,
    private userSession: UserSessionService
  ) {
    this.refreshCurrentUser();
    if (typeof window !== 'undefined') {
      window.addEventListener('storage', this.storageHandler);
    }
  }

  readonly currentUser$ = this._current$.asObservable();
  readonly users: readonly MockUser[] = MOCK_USERS;

  ngOnDestroy(): void {
    if (typeof window !== 'undefined') {
      window.removeEventListener('storage', this.storageHandler);
    }
  }

  currentUser(): MockUser | null {
    return this.refreshCurrentUser();
  }

  isLoggedIn(): boolean {
    return this.refreshCurrentUser() !== null;
  }

  getUserId(): number {
    return this.refreshCurrentUser()?.id ?? 0;
  }

  getRole(): MockUser['role'] | null {
    return this.refreshCurrentUser()?.role ?? null;
  }

  switchUser(id: number): void {
    if (isAuthenticated()) {
      return;
    }

    const user = MOCK_USERS.find((candidate) => candidate.id === id);
    if (!user) {
      return;
    }

    this._current$.next(user);
    try {
      localStorage.setItem('ybrainy_mock_user', JSON.stringify(user));
      window.dispatchEvent(
        new StorageEvent('storage', {
          key: 'ybrainy_mock_user',
          newValue: JSON.stringify(user),
        })
      );
    } catch {
      // ignore storage sync failures
    }

    if (user.role === 'STUDENT') {
      void this.router.navigate(['/courses']);
    } else {
      void this.router.navigate(['/dashboard']);
    }
  }

  logout(): void {
    try {
      localStorage.removeItem('ybrainy_mock_user');
    } catch {
      // ignore storage failures
    }

    this.userSession.clear();
    this._current$.next(null);

    if (isAuthenticated()) {
      void appLogout();
      return;
    }

    void this.router.navigate(['/']);
  }

  private refreshCurrentUser(): MockUser | null {
    const next = this.readMockUserFromStorage() ?? this.readAuthenticatedUser();
    const current = this._current$.getValue();

    if (!this.sameUser(current, next)) {
      this.zone.run(() => this._current$.next(next));
    }

    return next;
  }

  private readMockUserFromStorage(): MockUser | null {
    try {
      const raw = localStorage.getItem('ybrainy_mock_user');
      if (!raw) {
        return null;
      }

      const parsed = JSON.parse(raw) as Partial<MockUser>;
      if (!parsed || typeof parsed.id !== 'number' || !parsed.role) {
        return null;
      }

      return {
        id: parsed.id,
        name: typeof parsed.name === 'string' && parsed.name.trim() ? parsed.name : 'Mock User',
        role: String(parsed.role),
      };
    } catch {
      return null;
    }
  }

  private readAuthenticatedUser(): MockUser | null {
    if (!isAuthenticated()) {
      return null;
    }

    const session = this.userSession.get();
    const role = String(session?.role ?? getRealmRoles()[0] ?? '').toUpperCase();
    if (!role) {
      return null;
    }

    const name = session?.username || getDisplayName() || session?.email || 'Authenticated User';
    const id = typeof session?.userId === 'number' && Number.isFinite(session.userId) ? session.userId : 0;

    return { id, name, role };
  }

  private sameUser(left: MockUser | null, right: MockUser | null): boolean {
    if (left === right) {
      return true;
    }
    if (!left || !right) {
      return false;
    }

    return left.id === right.id && left.role === right.role && left.name === right.name;
  }
}
