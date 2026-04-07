import { Routes } from '@angular/router';
import { LoginPageComponent } from './login/login-page.component';
import { BackofficeDashboardComponent } from './backoffice/backoffice-dashboard.component';
import { SignUpPageComponent } from './signup/signup.component';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';
import { UsersComponent } from './backoffice/users/users.component';
import { adminGuard } from './auth/admin.guard';
import { loginRequiredGuard } from './auth/login-required.guard';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginPageComponent,
  },
  {
    path: 'signup',
    component: SignUpPageComponent,
  },
  {
    path: 'forgot-password',
    component: ForgotPasswordComponent,
  },
  {
    path: 'dashboard/categories',
    canActivate: [loginRequiredGuard],
    loadComponent: () =>
      import('./backoffice/category-list/category-list.component').then((m) => m.CategoryListComponent),
  },
  {
    path: 'dashboard/packs',
    canActivate: [loginRequiredGuard],
    loadComponent: () =>
      import('./backoffice/pack-list/pack-list.component').then((m) => m.PackListComponent),
  },
  {
    path: 'dashboard/packsorder',
    canActivate: [loginRequiredGuard],
    loadComponent: () =>
      import('./backoffice/packs-order/packs-order.component').then((m) => m.PacksOrderComponent),
  },
  {
    path: 'dashboard/finance',
    canActivate: [loginRequiredGuard],
    loadComponent: () =>
      import('./backoffice/finance/finance.component').then((m) => m.FinanceComponent),
  },
  {
    path: 'dashboard/courses',
    component: BackofficeDashboardComponent,
    data: { page: 'courses.html' },
    canActivate: [adminGuard],
  },
  {
    path: 'dashboard/lessons',
    component: BackofficeDashboardComponent,
    data: { page: 'lessons.html' },
    canActivate: [adminGuard],
  },
  {
    path: 'dashboard/calendar',
    component: BackofficeDashboardComponent,
    data: { page: 'app-calender.html' },
    canActivate: [adminGuard],
  },
  {
    path: 'dashboard/users',
    component: UsersComponent,
    canActivate: [adminGuard],
  },
  {
    path: 'dashboard/profile',
    canActivate: [adminGuard],
    loadComponent: () =>
      import('./frontoffice/pages/profile/profile.component').then((m) => m.ProfileComponent),
  },
  {
    path: 'dashboard',
    component: BackofficeDashboardComponent,
    canActivate: [adminGuard],
    data: { page: 'index.html' },
  },
  {
    path: '',
    loadChildren: () => import('./frontoffice/frontoffice.module').then((m) => m.FrontofficeModule)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
