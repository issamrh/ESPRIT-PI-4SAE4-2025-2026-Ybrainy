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
import { DraftsComponent } from './pages/forum/drafts/drafts.component';
import { ThreadDetailComponent } from './pages/forum/thread-detail/thread-detail.component';
import { CalendarComponent } from './pages/calendar/calendar.component';
import { CoursesComponent } from './pages/courses/courses.component';
import { LessonsComponent } from './pages/lessons/lessons.component';
import { TemplateMirrorComponent } from './pages/template-mirror/template-mirror.component';
import { frontofficeUserGuard } from '../auth/frontoffice-user.guard';
import { authGuard } from '../auth/auth.guard';
import { CourseDetailComponent } from './pages/course-detail/course-detail.component';
import { LessonDetailComponent } from './pages/lesson-detail/lesson-detail.component';
import { QuizPageComponent } from './pages/quiz-page/quiz-page.component';
import { CourseProgressComponent } from './pages/course-progress/course-progress.component';
import { CourseReviewsComponent } from './pages/course-reviews/course-reviews.component';
import { AiLearningPathPageComponent } from './pages/learning-paths/learning-paths.component';
import { StudentDashboardComponent } from './pages/student-dashboard/student-dashboard.component';
import { FoPackListComponent } from './pages/packs/pack-list/pack-list.component';
import { FoPackDetailComponent } from './pages/packs/pack-detail/pack-detail.component';
import { MessagingComponent } from './pages/messaging/messaging.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { WishlistComponent } from './pages/wishlist/wishlist.component';
import { UserProfileComponent } from './pages/user-profile/user-profile.component';

const routes: Routes = [
  {
    path: '',
    component: LayoutComponent,
    children: [
      { path: '', component: HomeComponent },
      { path: 'services/:slug', component: ServiceDetailComponent },
      { path: 'services', component: ServicesComponent },
      { path: 'payment', component: PaymentComponent, canActivate: [authGuard] },
      { path: 'resources', component: ResourcesComponent },
      { path: 'about', component: AboutComponent },
      { path: 'forum/create', component: CreateThreadComponent, canActivate: [authGuard] },
      { path: 'forum/drafts', component: DraftsComponent, canActivate: [authGuard] },
      { path: 'forum/profile', component: UserProfileComponent, canActivate: [authGuard] },
      { path: 'forum/:threadId', component: ThreadDetailComponent },
      { path: 'forum', component: ForumComponent },
      { path: 'wishlist', component: WishlistComponent, canActivate: [authGuard] },
      { path: 'messages', component: MessagingComponent, canActivate: [authGuard] },
      { path: 'messages/:userId', component: MessagingComponent, canActivate: [authGuard] },
      { path: 'my-dashboard', component: DashboardComponent, canActivate: [authGuard] },
      { path: 'calendar', component: CalendarComponent },
      { path: 'courses', component: CoursesComponent },
      { path: 'courses/:courseId/lessons/:lessonId', component: LessonDetailComponent },
      { path: 'courses/:courseId/lessons', component: LessonsComponent },
      { path: 'courses/:courseId/progress', component: CourseProgressComponent, canActivate: [authGuard] },
      { path: 'courses/:courseId/quiz', component: QuizPageComponent, canActivate: [authGuard] },
      { path: 'courses/:courseId/reviews', component: CourseReviewsComponent, canActivate: [authGuard] },
      { path: 'courses/:courseId', component: CourseDetailComponent },
      { path: 'packks', redirectTo: 'packs', pathMatch: 'full' },
      { path: 'packs', component: FoPackListComponent },
      { path: 'packs/:id', component: FoPackDetailComponent },
      { path: 'learning-paths', component: AiLearningPathPageComponent },
      { path: 'my-learning', component: StudentDashboardComponent, canActivate: [authGuard] },
      {
        path: 'profile',
        canActivate: [frontofficeUserGuard],
        loadComponent: () =>
          import('./pages/profile/profile.component').then((m) => m.ProfileComponent),
      },
      { path: 'template', component: TemplateMirrorComponent },
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FrontofficeRoutingModule { }
