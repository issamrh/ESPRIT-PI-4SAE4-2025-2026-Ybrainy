import { Routes } from '@angular/router';
import { LoginPageComponent } from './login/login-page.component';
import { BackofficeDashboardComponent } from './backoffice/backoffice-dashboard.component';
import { BackofficeForumThreadsComponent } from './backoffice/forum/backoffice-forum-threads.component';
import { BackofficeForumThreadDetailComponent } from './backoffice/forum/backoffice-forum-thread-detail.component';
import { BackofficeCategoriesComponent } from './backoffice/categories/backoffice-categories.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginPageComponent,
  },
  {
    path: 'dashboard/categories',
    component: BackofficeCategoriesComponent,
  },
  {
    path: 'dashboard/forum/threads',
    component: BackofficeForumThreadsComponent,
  },
  {
    path: 'dashboard/forum/threads/:threadId',
    component: BackofficeForumThreadDetailComponent,
  },
  {
    path: 'dashboard/courses',
    component: BackofficeDashboardComponent,
    data: { page: 'courses.html' },
  },
  {
    path: 'dashboard/lessons',
    component: BackofficeDashboardComponent,
    data: { page: 'lessons.html' },
  },
  {
    path: 'dashboard/calendar',
    component: BackofficeDashboardComponent,
    data: { page: 'app-calender.html' },
  },
  {
    path: 'dashboard',
    component: BackofficeDashboardComponent,
    data: { page: 'index.html' },
  },
  {
    path: '',
    loadChildren: () => import('./frontoffice/frontoffice.module').then(m => m.FrontofficeModule)
  },
  {
    path: '**',
    redirectTo: ''
  }
];
