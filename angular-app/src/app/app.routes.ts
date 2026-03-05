import { Routes } from '@angular/router';
import { LoginPageComponent } from './login/login-page.component';
import { BackofficeDashboardComponent } from './backoffice/backoffice-dashboard.component';
import { FinanceComponent } from './backoffice/finance/finance.component';
import { CategoryListComponent } from './backoffice/category-list/category-list.component';
import { CategoryFormComponent } from './backoffice/category-form/category-form.component';
import { PackListComponent } from './backoffice/pack-list/pack-list.component';
import { PackFormComponent } from './backoffice/pack-form/pack-form.component';
import { PacksOrderComponent } from './backoffice/packs-order/packs-order.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginPageComponent,
  },
  // Backoffice: Pack Categories
  {
    path: 'dashboard/categories',
    component: CategoryListComponent,
    canActivate: [authGuard],
  },
  {
    path: 'dashboard/categories/new',
    component: CategoryFormComponent,
    canActivate: [authGuard],
  },
  {
    path: 'dashboard/categories/edit/:id',
    component: CategoryFormComponent,
    canActivate: [authGuard],
  },
  // Backoffice: Packs
  {
    path: 'dashboard/packs',
    component: PackListComponent,
    canActivate: [authGuard],
  },
  {
    path: 'dashboard/packs/new',
    component: PackFormComponent,
    canActivate: [authGuard],
  },
  {
    path: 'dashboard/packs/edit/:id',
    component: PackFormComponent,
    canActivate: [authGuard],
  },
  {
    path: 'dashboard/packsorder',
    component: PacksOrderComponent,
    canActivate: [authGuard],
  },
  // Backoffice: Existing routes
  {
    path: 'dashboard/finance',
    component: FinanceComponent,
    canActivate: [authGuard],
  },
  {
    path: 'dashboard/courses',
    component: BackofficeDashboardComponent,
    canActivate: [authGuard],
    data: { page: 'courses.html' },
  },
  {
    path: 'dashboard/lessons',
    component: BackofficeDashboardComponent,
    canActivate: [authGuard],
    data: { page: 'lessons.html' },
  },
  {
    path: 'dashboard/calendar',
    component: BackofficeDashboardComponent,
    canActivate: [authGuard],
    data: { page: 'app-calender.html' },
  },
  {
    path: 'dashboard/e-learning-packs',
    component: BackofficeDashboardComponent,
    canActivate: [authGuard],
    data: { page: 'ecom-product-list.html' },
  },
  {
    path: 'dashboard',
    component: BackofficeDashboardComponent,
    canActivate: [authGuard],
    data: { page: 'index.html' },
  },
  // Frontoffice
  {
    path: '',
    loadChildren: () => import('./frontoffice/frontoffice.module').then(m => m.FrontofficeModule)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
