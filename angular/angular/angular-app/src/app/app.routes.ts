import { Routes } from '@angular/router';
import { LoginPageComponent } from './login/login-page.component';
import { BackofficeDashboardComponent } from './backoffice/backoffice-dashboard.component';
import { CategoryFormComponent } from './backoffice/category-form/category-form.component';
import { CategoryListComponent } from './backoffice/category-list/category-list.component';
import { FinanceComponent } from './backoffice/finance/finance.component';
import { PackFormComponent } from './backoffice/pack-form/pack-form.component';
import { PackListComponent } from './backoffice/pack-list/pack-list.component';
import { PacksOrderComponent } from './backoffice/packs-order/packs-order.component';
import { SignUpPageComponent } from './signup/signup.component';
import { ForgotPasswordComponent } from './forgot-password/forgot-password.component';
import { UsersComponent } from './backoffice/users/users.component';
import { adminGuard } from './auth/admin.guard';

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
    component: CategoryListComponent,
    canActivate: [adminGuard],
  },
  {
    path: 'dashboard/categories/new',
    component: CategoryFormComponent,
    canActivate: [adminGuard],
  },
  {
    path: 'dashboard/categories/edit/:id',
    component: CategoryFormComponent,
    canActivate: [adminGuard],
  },
  {
    path: 'dashboard/packs',
    component: PackListComponent,
    canActivate: [adminGuard],
  },
  {
    path: 'dashboard/packs/new',
    component: PackFormComponent,
    canActivate: [adminGuard],
  },
  {
    path: 'dashboard/packs/edit/:id',
    component: PackFormComponent,
    canActivate: [adminGuard],
  },
  {
    path: 'dashboard/packsorder',
    component: PacksOrderComponent,
    canActivate: [adminGuard],
  },
  {
    path: 'dashboard/finance',
    component: FinanceComponent,
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
    path: 'dashboard',
    component: BackofficeDashboardComponent,
    data: { page: 'index.html' },
    canActivate: [adminGuard],
  },
  {
    path: '',
    loadChildren: () => import('./frontoffice/frontoffice.module').then(m => m.FrontofficeModule),
  },
  {
    path: '**',
    redirectTo: ''
  }
];
