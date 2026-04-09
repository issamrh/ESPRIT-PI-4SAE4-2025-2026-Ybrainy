import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { LayoutComponent } from './layout/layout.component';
import { HomeComponent } from './home/home.component';
import { ServicesComponent } from './pages/services/services.component';
import { ServiceDetailComponent } from './pages/service-detail/service-detail.component';
import { PaymentComponent } from './pages/payment/payment.component';
import { ResourcesComponent } from './pages/resources/resources.component';
import { AboutComponent } from './pages/about/about.component';
import { ForumComponent } from './pages/forum/forum.component';
import { CreateThreadComponent } from './pages/forum/create-thread/create-thread.component';
import { ThreadDetailComponent } from './pages/forum/thread-detail/thread-detail.component';
import { CalendarComponent } from './pages/calendar/calendar.component';
import { CoursesComponent } from './pages/courses/courses.component';
import { LessonsComponent } from './pages/lessons/lessons.component';
import { TemplateMirrorComponent } from './pages/template-mirror/template-mirror.component';
import { UserProfileComponent } from './pages/user-profile/user-profile.component';
import { WishlistComponent } from './pages/wishlist/wishlist.component';
import { DraftsComponent } from './pages/forum/drafts/drafts.component';
import { MessagingComponent } from './pages/messaging/messaging.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { authGuard } from '../auth.guard';

const routes: Routes = [
  {
    path: '',
    component: LayoutComponent,
    children: [
      { path: '', component: HomeComponent },
      { path: 'services/:slug', component: ServiceDetailComponent },
      { path: 'services', component: ServicesComponent },
      { path: 'payment', component: PaymentComponent },
      { path: 'resources', component: ResourcesComponent },
      { path: 'about', component: AboutComponent },
      { path: 'forum', component: ForumComponent, canActivate: [authGuard] },
      { path: 'forum/create', component: CreateThreadComponent, canActivate: [authGuard] },
      { path: 'forum/drafts', component: DraftsComponent, canActivate: [authGuard] },
      { path: 'forum/:threadId', component: ThreadDetailComponent, canActivate: [authGuard] },
      { path: 'profile', component: UserProfileComponent, canActivate: [authGuard] },
      { path: 'wishlist', component: WishlistComponent, canActivate: [authGuard] },
      { path: 'messages', component: MessagingComponent, canActivate: [authGuard] },
      { path: 'messages/:userId', component: MessagingComponent, canActivate: [authGuard] },
      { path: 'my-dashboard', component: DashboardComponent, canActivate: [authGuard] },
      { path: 'calendar', component: CalendarComponent },
      { path: 'courses', component: CoursesComponent },
      { path: 'courses/:courseId/lessons', component: LessonsComponent },
      { path: 'template', component: TemplateMirrorComponent },
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FrontofficeRoutingModule { }
