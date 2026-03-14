// ─────────────────────────────────────────────────────────────────────────────
// Additions to your existing app.component.ts (or header.component.ts)
// Add these properties and methods to your existing component class
// ─────────────────────────────────────────────────────────────────────────────

// ── New properties to add ──────────────────────────────────────────────────
profileOpen = false;
profilePicture: string | null = null;
userRole: string = '';
userStatus: string = 'SECURE';
streakDays: number = 0;

// ── Add to ngOnInit (after your existing auth init) ────────────────────────
// Call this after you confirm the user is authenticated:
loadHeaderUserData(): void {
  this.userService.getCurrentUser().subscribe({
    next: (user) => {
      this.profilePicture  = user.profilePicture ?? null;
      this.displayName     = user.firstName ? `${user.firstName} ${user.lastName}` : user.username;
      this.userRole        = user.role;
      this.userStatus      = user.status ?? 'SECURE';
      this.streakDays      = user.streakDays ?? 0;
    }
  });
}

// ── New helper method ──────────────────────────────────────────────────────
getInitials(): string {
  if (!this.displayName) return '?';
  const parts = this.displayName.trim().split(' ');
  return parts.length >= 2
    ? (parts[0][0] + parts[parts.length - 1][0]).toUpperCase()
    : parts[0][0].toUpperCase();
}

// ── Click-outside directive (add to your module or standalone imports) ─────
// Option A: use an existing ClickOutside directive from your project
// Option B: use host listener inside the dropdown logic:
//
//   @HostListener('document:click', ['$event'])
//   onDocumentClick(event: MouseEvent) {
//     const target = event.target as HTMLElement;
//     if (!target.closest('.profile-menu-wrapper')) {
//       this.profileOpen = false;
//     }
//   }
//
// Add the @HostListener to the component class, and remove (clickOutside) from HTML,
// replacing it with nothing (the HostListener handles it globally).


// ─────────────────────────────────────────────────────────────────────────────
// Route — add to your app.routes.ts
// ─────────────────────────────────────────────────────────────────────────────
/*
  {
    path: 'profile',
    loadComponent: () =>
      import('./profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [AuthGuard]
  },
*/


// ─────────────────────────────────────────────────────────────────────────────
// UserService — add getCurrentUser() if not already present
// ─────────────────────────────────────────────────────────────────────────────
/*
  getCurrentUser(): Observable<UserProfile> {
    // If you store the userId in the JWT / local state:
    const userId = this.keycloakService.getUserId(); // or from your auth store
    return this.http.get<UserProfile>(`${environment.apiUrl}/api/users/${userId}`);
  }

  updateUser(id: number, dto: Partial<UserProfile>): Observable<UserProfile> {
    return this.http.put<UserProfile>(`${environment.apiUrl}/api/users/${id}`, dto);
  }

  deleteUser(id: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/users/${id}`);
  }
*/
