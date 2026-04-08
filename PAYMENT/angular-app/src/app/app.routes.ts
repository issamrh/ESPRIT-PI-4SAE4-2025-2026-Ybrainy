import { Routes } from '@angular/router';
import { LoginPageComponent } from './login/login-page.component';
import { BackofficeDashboardComponent } from './backoffice/backoffice-dashboard.component';
import { FinanceComponent } from './backoffice/finance/finance.component';
import { CategoryListComponent } from './backoffice/category-list/category-list.component';
import { CategoryFormComponent } from './backoffice/category-form/category-form.component';
import { PackListComponent } from './backoffice/pack-list/pack-list.component';
import { PackFormComponent } from './backoffice/pack-form/pack-form.component';
import { PacksOrderComponent } from './backoffice/packs-order/packs-order.component';
import { AuthGuard } from './guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginPageComponent,
  },
  // Backoffice: Pack Categories
  {
    path: 'dashboard/categories',
    component: CategoryListComponent,
    canActivate: [AuthGuard],
  },
  {
    path: 'dashboard/categories/new',
    component: CategoryFormComponent,
    canActivate: [AuthGuard],
  },
  {
    path: 'dashboard/categories/edit/:id',
    component: CategoryFormComponent,
    canActivate: [AuthGuard],
  },
  // Backoffice: Packs
  {
    path: 'dashboard/packs',
    component: PackListComponent,
    canActivate: [AuthGuard],
  },
  {
    path: 'dashboard/packs/new',
    component: PackFormComponent,
    canActivate: [AuthGuard],
  },
  {
    path: 'dashboard/packs/edit/:id',
    component: PackFormComponent,
    canActivate: [AuthGuard],
  },
  {
    path: 'dashboard/packsorder',
    component: PacksOrderComponent,
    canActivate: [AuthGuard],
  },
  // Backoffice: Existing routes
  {
    path: 'dashboard/finance',
    component: FinanceComponent,
    canActivate: [AuthGuard],
  },
  {
    path: 'dashboard/courses',
    component: BackofficeDashboardComponent,
    canActivate: [AuthGuard],
    data: { page: 'courses.html' },
  },
  {
    path: 'dashboard/lessons',
    component: BackofficeDashboardComponent,
    canActivate: [AuthGuard],
    data: { page: 'lessons.html' },
  },
  {
    path: 'dashboard/calendar',
    component: BackofficeDashboardComponent,
    canActivate: [AuthGuard],
    data: { page: 'app-calender.html' },
  },
  {
    path: 'dashboard/e-learning-packs',
    component: BackofficeDashboardComponent,
    canActivate: [AuthGuard],
    data: { page: 'ecom-product-list.html' },
  },
  {
    path: 'dashboard',
    component: BackofficeDashboardComponent,
    canActivate: [AuthGuard],
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
