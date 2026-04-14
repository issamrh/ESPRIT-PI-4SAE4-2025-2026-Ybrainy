import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, Observable, tap } from 'rxjs';

export interface AuthUser {
  id: number;
  username: string;
  email: string;
  role: string;
  xp: number;
  level: number;
}

const STORAGE_KEY = 'forum_auth_user';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly userSubject: BehaviorSubject<AuthUser | null>;
  readonly currentUser$: Observable<AuthUser | null>;

  constructor(private http: HttpClient) {
    const stored = localStorage.getItem(STORAGE_KEY);
    const initial: AuthUser | null = stored ? JSON.parse(stored) : null;
    this.userSubject = new BehaviorSubject<AuthUser | null>(initial);
    this.currentUser$ = this.userSubject.asObservable();
  }

  get currentUser(): AuthUser | null {
    return this.userSubject.value;
  }

  get currentUserId(): number | null {
    return this.userSubject.value?.id ?? null;
  }

  isLoggedIn(): boolean {
    return this.userSubject.value !== null;
  }

  login(usernameOrEmail: string, password: string): Observable<AuthUser> {
    const identifier = (usernameOrEmail ?? '').trim();
    const payload = identifier.includes('@')
      ? { email: identifier, password }
      : { username: identifier, password };

    return this.http.post<AuthUser>('/api/auth/login', payload).pipe(
      tap((user) => {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(user));
        this.userSubject.next(user);
      })
    );
  }

  logout(): void {
    this.http.post('/api/auth/logout', {}).subscribe({ error: () => {} });
    localStorage.removeItem(STORAGE_KEY);
    this.userSubject.next(null);
  }

  /** Refresh stored user fields (xp, level) after XP changes */
  updateUser(partial: Partial<AuthUser>): void {
    const current = this.userSubject.value;
    if (!current) return;
    const updated = { ...current, ...partial };
    localStorage.setItem(STORAGE_KEY, JSON.stringify(updated));
    this.userSubject.next(updated);
  }
}
