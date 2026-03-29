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
import { CalendarComponent } from './pages/calendar/calendar.component';
import { CoursesComponent } from './pages/courses/courses.component';
import { CourseDetailComponent } from './pages/course-detail/course-detail.component';
import { LessonsComponent } from './pages/lessons/lessons.component';
import { LessonDetailComponent } from './pages/lesson-detail/lesson-detail.component';
import { CourseProgressComponent } from './pages/course-progress/course-progress.component';
import { QuizPageComponent } from './pages/quiz-page/quiz-page.component';
import { CourseReviewsComponent } from './pages/course-reviews/course-reviews.component';
import { TemplateMirrorComponent } from './pages/template-mirror/template-mirror.component';
import { AiLearningPathPageComponent } from './pages/learning-paths/learning-paths.component';
import { CertificateVerifyComponent } from './pages/certificate-verify/certificate-verify.component';
import { StudentDashboardComponent } from './pages/student-dashboard/student-dashboard.component';
import { FoPackListComponent } from './pages/packs/pack-list/pack-list.component';
import { FoPackDetailComponent } from './pages/packs/pack-detail/pack-detail.component';
import { frontofficeUserGuard } from '../auth/frontoffice-user.guard';

const routes: Routes = [
  {
    path: 'verify/:id',
    component: CertificateVerifyComponent,
  },
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
      { path: 'forum', component: ForumComponent },
      { path: 'calendar', component: CalendarComponent },
      { path: 'courses', component: CoursesComponent },
      { path: 'courses/:courseId/lessons', component: LessonsComponent },
      { path: 'courses/:courseId/lessons/:lessonId', component: LessonDetailComponent },
      { path: 'courses/:courseId/progress', component: CourseProgressComponent },
      { path: 'courses/:courseId/quiz', component: QuizPageComponent },
      { path: 'courses/:courseId/reviews', component: CourseReviewsComponent },
      { path: 'courses/:courseId', component: CourseDetailComponent },
      { path: 'packs', component: FoPackListComponent },
      { path: 'packs/:id', component: FoPackDetailComponent },
      { path: 'template', component: TemplateMirrorComponent },
      { path: 'learning-paths', component: AiLearningPathPageComponent },
      { path: 'my-learning', component: StudentDashboardComponent },
      {
        path: 'profile',
        canActivate: [frontofficeUserGuard],
        loadComponent: () =>
          import('./pages/profile/profile.component').then((m) => m.ProfileComponent),
      },
    ]
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FrontofficeRoutingModule { }
