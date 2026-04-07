import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { Router } from '@angular/router';

export interface MockUser {
  id:   number;
  name: string;
  role: 'STUDENT' | 'INSTRUCTOR' | 'ADMIN';
}

const MOCK_USERS: MockUser[] = [
  { id: 1,   name: 'Alice (Student)',    role: 'STUDENT'    },
  { id: 2,   name: 'Bob (Student)',      role: 'STUDENT'    },
  { id: 100, name: 'John (Instructor)',  role: 'INSTRUCTOR' },
  { id: 101, name: 'Sarah (Instructor)', role: 'INSTRUCTOR' },
  { id: 999, name: 'Admin',             role: 'ADMIN'      },
];

@Injectable({ providedIn: 'root' })
export class AuthMockService {
  // Starts as null (no user selected) — use the Ctrl+U switcher to pick a role
  private readonly _current$ = new BehaviorSubject<MockUser | null>(null);

  constructor(private router: Router) {}

  readonly currentUser$ = this._current$.asObservable();
  readonly users: readonly MockUser[] = MOCK_USERS;

  currentUser(): MockUser | null {
    return this._current$.getValue();
  }

  isLoggedIn(): boolean {
    return this._current$.getValue() !== null;
  }

  getUserId(): number {
    return this._current$.getValue()?.id ?? 0;
  }

  getRole(): 'STUDENT' | 'INSTRUCTOR' | 'ADMIN' | null {
    return this._current$.getValue()?.role ?? null;
  }

  switchUser(id: number): void {
    const user = MOCK_USERS.find(u => u.id === id);
    if (!user) return;
    this._current$.next(user);
    try { localStorage.setItem('ybrainy_mock_user', JSON.stringify(user)); } catch {}
    try {
      window.dispatchEvent(new StorageEvent('storage', {
        key: 'ybrainy_mock_user',
        newValue: JSON.stringify(user)
      }));
    } catch {}
    if (user.role === 'STUDENT') {
      this.router.navigate(['/courses']);
    } else {
      this.router.navigate(['/dashboard']);
    }
  }

  logout(): void {
    this._current$.next(null);
  }
}
